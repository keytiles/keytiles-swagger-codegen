package com.keytiles.swagger.codegen.helper;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
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

	private CodegenBugfixAndEnhanceHelper() {
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
			for (CodegenProperty property : modelEntry.getValue().vars) {

				// does it do a $ref ?
				if (property.jsonSchema.contains("$ref")) {

					// OK this is a property which is referencing in another model
					HashMap<String, Object> parseResult = null;
					Exception parsingException = null;

					try {
						parseResult = objectMapper.readValue(property.jsonSchema, HashMap.class);
					} catch (Exception e) {
						parsingException = e;
					}
					Preconditions.checkState(parseResult != null,
							"Oops it looks we failed to json parse $ref attribute at property %s.%s! jsonSchema\n%s\nlead to error: %s",
							modelEntry.getKey(), property.baseName, property.jsonSchema, parsingException);

					String referredModelName = FilenameUtils.getName((String) parseResult.get("$ref"));
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

						// === let's inherit the 'nullable' thing if set

						Object referredModelNullableAttrib = referredModel.vendorExtensions
								.get(CodegenConstants.IS_NULLABLE_EXT_NAME);
						if (referredModelNullableAttrib != null) {
							property.nullable = referredModel.getIsNullable();
							property.vendorExtensions.put(CodegenConstants.IS_NULLABLE_EXT_NAME,
									referredModelNullableAttrib);

							PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
									"field refers to '" + referredModelName + "' which is 'nullable="
											+ referredModelNullableAttrib + "' so this is inherited into this field");
						}

						// === let's inherit the 'readOnly' thing - if it has any setup

						Object referredModelIsReadonlyAttrib = referredModel.vendorExtensions
								.get(CodegenConstants.IS_READ_ONLY_EXT_NAME);
						if (referredModelIsReadonlyAttrib != null) {
							property.vendorExtensions.put(CodegenConstants.IS_READ_ONLY_EXT_NAME,
									referredModelIsReadonlyAttrib);

							PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
									"field refers to '" + referredModelName + "' which is 'readOnly="
											+ referredModelIsReadonlyAttrib + "' so this is inherited into this field");
						}

						// === if enum then let's inherit default value - if set

						if (referredModel.getIsEnum()) {
							parsingException = null;
							try {
								parseResult = objectMapper.readValue(referredModel.modelJson, HashMap.class);
							} catch (Exception e) {
								parsingException = e;
							}
							Preconditions.checkState(parseResult != null,
									"Oops it looks we failed to json parse the .modelJson of $ref referred model '%s'... modelJson\n%s\nlead to error: %s",
									referredModel.name, referredModel.modelJson, parsingException);

							// does it have a default?
							Object defaultValue = parseResult.get("default");
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

}
