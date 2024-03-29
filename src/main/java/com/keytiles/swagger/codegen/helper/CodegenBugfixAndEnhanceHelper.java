package com.keytiles.swagger.codegen.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.error.SchemaValidationException;
import com.keytiles.swagger.codegen.helper.debug.ModelMessageType;
import com.keytiles.swagger.codegen.helper.debug.PropertyInlineMessages;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;

/**
 * Static util class which helps to fix bugs present in original swagger-codegen or do things better
 *
 * @author attilaw
 *
 */
public class CodegenBugfixAndEnhanceHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(CodegenBugfixAndEnhanceHelper.class);

	private CodegenBugfixAndEnhanceHelper() {
	}

	/**
	 * For some reason the .isComposedModel property is not set for certain models which are definitely
	 * a composition of others - so using anyOf/oneOf/allOf in its schema. This method recognizing even
	 * these as composed models
	 *
	 * @param theModel
	 * @return TRUE if the model is composed model - FALSE otherwise
	 */
	public static boolean isComposedModel(CodegenModel theModel) {
		if (theModel.isComposedModel) {
			return true;
		}

		HashMap<String, Object> modelJson = CodegenUtil.getParsedModelJson(theModel);
		for (Map.Entry<String, Object> entry : modelJson.entrySet()) {
			if ("anyOf".equalsIgnoreCase(entry.getKey()) || "oneOf".equalsIgnoreCase(entry.getKey())
					|| "allOf".equalsIgnoreCase(entry.getKey())) {
				return true;
			}
		}

		return false;
	}

	public static void validateOnlySupportedVendorAttributesAreUsedOnModel(IKeytilesCodegen codegen,
			CodegenModel model) {
		Set<String> supportedAttrs = codegen.getAllSupportedObjectLevelVendorFieldNames();
		// let's add a few which are actually added by code and not user
		supportedAttrs.add(IKeytilesCodegen.X_MODEL_IS_OWN_MODEL);
		supportedAttrs.add(IKeytilesCodegen.X_MODEL_STATE);

		model.getVendorExtensions().keySet().forEach(key -> {
			Preconditions.checkState(
					key.startsWith(IKeytilesCodegen.VENDOR_PREFIX) == false || supportedAttrs.contains(key),
					"'%s' was found on model %s which is not supported in this version. Please remove/replace this vendor attribute!",
					key, model.name);
		});
	}

	public static void validateOnlySupportedVendorAttributesAreUsedOnModelProperty(IKeytilesCodegen codegen,
			CodegenModel model, CodegenProperty property) {
		Set<String> supportedAttrs = codegen.getAllSupportedPropertyLevelVendorFieldNames();
		property.getVendorExtensions().keySet().forEach(key -> {
			Preconditions.checkState(
					key.startsWith(IKeytilesCodegen.VENDOR_PREFIX) == false || supportedAttrs.contains(key),
					"'%s' was found on property %s.%s which is not supported in this version. Please remove/replace this vendor attribute!",
					key, model.name, property.baseName);
		});
	}

	/**
	 * It looks there is a problem in codegen if a property is using "$ref" element... Due to
	 * https://swagger.io/docs/specification/using-ref/, "$ref and Sibling Elements" section when one is
	 * using $ref the referred schema should be used "as is". Like I wrote there that (as an include)
	 * <p>
	 * But it looks codegen does not really do that... Attributes like 'nullable' or 'readOnly' or even
	 * 'default' value (makes only sense if referred in model is an Enum) simply just ignored.
	 * <p>
	 * If you have
	 *
	 * <pre>
	 * ReferredNullableEnumWithDefault:
	 *   type: string
	 *   enum: [apple, orange, mango]
	 *   default: orange
	 *   nullable: true
	 *
	 * ReferringClass:
	 *   type: object
	 *   properties:
	 *     referredNullableEnumField:
	 *       $ref: '#/components/schemas/ReferredNullableEnumWithDefault'
	 * </pre>
	 *
	 * The ReferringClass.referredNullableEnumField should be generated the way like I would write this:
	 *
	 * <pre>
	 * ReferringClass:
	 *   type: object
	 *   properties:
	 *     referredNullableEnumField:
	 *       type: string
	 *       enum: [apple, orange, mango]
	 *       default: orange
	 *       nullable: true
	 * </pre>
	 *
	 * This method is fixing this problem
	 *
	 */
	public static void fixReferredModelAttributesInheritance(Map<String, CodegenModel> allModels) {
		ObjectMapper objectMapper = new ObjectMapper();

		Map<String, CodegenModel> allModelsByInternalName = new HashMap<>();
		allModels.entrySet().forEach(modelEntry -> {
			allModelsByInternalName.put(modelEntry.getValue().name, modelEntry.getValue());
		});

		allModels.entrySet().forEach(modelEntry -> {
			// if ("ReferringClass".equals(modelEntry.getKey())) {
			// System.out.println("buuu");
			// }

			for (CodegenProperty property : modelEntry.getValue().vars) {

				// does it do a $ref ?
				if (property.jsonSchema.contains("$ref")) {

					// OK this is a property which is referencing in another model
					HashMap<String, Object> parsedJsonSchema = null;
					Exception parsingException = null;

					try {
						parsedJsonSchema = objectMapper.readValue(property.jsonSchema, HashMap.class);
					} catch (Exception e) {
						parsingException = e;
					}
					Preconditions.checkState(parsedJsonSchema != null,
							"Oops it looks we failed to json parse $ref attribute at property %s.%s! jsonSchema\n%s\nlead to error: %s",
							modelEntry.getKey(), property.baseName, property.jsonSchema, parsingException);

					String referredModelName = FilenameUtils.getName((String) parsedJsonSchema.get("$ref"));
					// it is possible we have NULL now... if schema is using "anyOf", "oneOf", etc etc
					// it that is the case then lets just skip that
					if (referredModelName != null) {
						CodegenModel referredModel = allModels.get(referredModelName);
						if (referredModel == null) {
							// as a fallback (inline defined objects/enums can end up in this) let's look with the
							// raw generated name too
							referredModel = allModelsByInternalName.get(referredModelName);
						}
						Preconditions.checkState(referredModel != null,
								"It looks %s.%s is $ref in '%s' but we could not find this referred model as CodegenModel - this really should not be a case probably it is a bug!",
								modelEntry.getKey(), property.baseName, referredModelName);

						// now let's parse the modelJson up
						parsingException = null;
						HashMap<String, Object> parsedModelJson = null;
						try {
							parsedModelJson = objectMapper.readValue(referredModel.modelJson, HashMap.class);
						} catch (Exception e) {
							parsingException = e;
						}
						Preconditions.checkState(parsedModelJson != null,
								"Oops it looks we failed to json parse the .modelJson of $ref referred model '%s'... modelJson\n%s\nlead to error: %s",
								referredModel.name, referredModel.modelJson, parsingException);

						// === let's inherit the 'nullable' thing if set

						// Object referredModelNullableAttrib__ = referredModel.vendorExtensions
						// .get(CodegenConstants.IS_NULLABLE_EXT_NAME);
						Object referredModelNullableAttrib = parsedModelJson.get("nullable");
						if (referredModelNullableAttrib != null) {
							property.nullable = (Boolean) referredModelNullableAttrib;
							property.vendorExtensions.put(CodegenConstants.IS_NULLABLE_EXT_NAME,
									referredModelNullableAttrib);

							PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
									"field refers to '" + referredModelName + "' which is 'nullable="
											+ referredModelNullableAttrib + "' so this is inherited into this field");
						}

						// === let's inherit the 'readOnly' thing - if it has any setup

						// Object referredModelIsReadonlyAttrib__ = referredModel.vendorExtensions
						// .get(CodegenConstants.IS_READ_ONLY_EXT_NAME);
						Object referredModelIsReadonlyAttrib = parsedModelJson.get("readOnly");
						if (referredModelIsReadonlyAttrib != null) {
							property.vendorExtensions.put(CodegenConstants.IS_READ_ONLY_EXT_NAME,
									referredModelIsReadonlyAttrib);

							PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
									"field refers to '" + referredModelName + "' which is 'readOnly="
											+ referredModelIsReadonlyAttrib + "' so this is inherited into this field");
						}

						// === if enum then let's inherit default value - if set

						if (referredModel.getIsEnum()) {
							// does it have a default?
							Object defaultValue = parsedModelJson.get("default");
							if (defaultValue != null) {
								// cool, let's inherit!
								String strForm = "" + defaultValue;
								if (defaultValue instanceof String) {
									strForm = "\"" + strForm + "\"";
								}

								property.defaultValue = referredModel.classname + ".fromValue(" + strForm + ")";

								PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
										"field refers to '" + referredModelName
												+ "' which is an Enum with default value " + strForm
												+ " - this is inherited into this field");
							}
						}
					}
				}
			}
		});
	}

	public static void validateModelsAgainstKnownContradictions(Map<String, CodegenModel> allModels) {

		allModels.entrySet().forEach(modelEntry -> {

			// if ("ExtendedErrorCodesAnyOf".equals(modelEntry.getKey())) {
			// System.out.println("buuu");
			// }

			if (isComposedModel(modelEntry.getValue())) {
				HashMap<String, Object> modelJson = CodegenUtil.getParsedModelJson(modelEntry.getValue());
				String type = (String) modelJson.get("type");
				if (type != null && !"object".equalsIgnoreCase(type)) {

					// OK so this is a composed model with a direct 'type' declaration
					// codegen only works well for type:object declarations

					throw new SchemaValidationException("model '" + modelEntry.getKey()
							+ "' is a composed model (anyOf, oneOf, allOf) but its definition contains 'type: " + type
							+ "' declaration - Codegen just works well for 'type: object' compositions. Do you really need that type declaration?");
				}
			}
		});
	}

	/**
	 * This method is checking the model - whether it is defined directly in the schema we are
	 * generating, or just imported from another schema file. The "own" models are marked with
	 * {@link IKeytilesCodegen#X_MODEL_IS_OWN_MODEL} = TRUE
	 *
	 */
	public static void markOwnModel(CodegenModel model, IKeytilesCodegen codegen) {

		// note: for now we just do something stupid string search... but should do
		boolean isOwnDefinedModel = codegen.getInputSpec().contains(" " + model.name + ":");
		model.vendorExtensions.put(IKeytilesCodegen.X_MODEL_IS_OWN_MODEL, isOwnDefinedModel);

		if (isOwnDefinedModel && CodegenUtil.isModelFabricatedModel(model)) {
			LOGGER.info("buu ==== contradicting own: {}", model);
		}
	}

	/**
	 * Helper method to quickly ask if a model is own model or not - see
	 * {@link #markOwnModel(CodegenModel, IKeytilesCodegen)}!
	 *
	 * @param model
	 * @return TRUE if yes FALSE if not
	 * @throws IllegalStateException
	 *             in case the marker flag is missing
	 */
	public static boolean isOwnModel(CodegenModel model) {
		Boolean isOwn = (Boolean) model.vendorExtensions.get(IKeytilesCodegen.X_MODEL_IS_OWN_MODEL);
		Preconditions.checkState(isOwn != null,
				"Oops! It looks this model '%s' was not going through the %s.markOwnModel() method earlier... How is it possible? Did you create this by hand?",
				model, CodegenBugfixAndEnhanceHelper.class.getSimpleName());
		return isOwn;
	}

	/**
	 * We can add importMappings - fine. This is a supported feature. But that import mapping works
	 * really dumb... It's just a name of the model. Without info from where it was coming from. If our
	 * own schema contains an own model with the same name we might seriously obfuscate things.
	 * Therefore it is better not to allow this and fail the build in this case
	 * <p>
	 * note: it is possible this could be enhanced in the future but for now let's just be on the safe
	 * side and do not allow this
	 *
	 * @param allModels
	 * @param codegen
	 */
	public static void ensureNoConflictBetweenNameOfOwnModelsAndImportedModels(Map<String, CodegenModel> allModels,
			IKeytilesCodegen codegen) {

		for (CodegenModel model : allModels.values()) {
			String importMappingPackageName = codegen.importMapping().get(model.name);
			if (importMappingPackageName != null) {
				boolean isOwnDefinedModel = isOwnModel(model);
				if (isOwnDefinedModel) {
					// we have a conflict!
					throw new SchemaValidationException("model '" + model.name
							+ "' is defined in the schema but there is a model in importMappings too with the same name (coming from package '"
							+ importMappingPackageName
							+ "') - this is not really safe... As importMappings in original Codegen just works based on model names without considering the source (ppackage) and this way there is no possibility to distinguish btw imported models and defined models... So please rename this model either in the import place or in the schema!");
				}

			}
		}
	}

}
