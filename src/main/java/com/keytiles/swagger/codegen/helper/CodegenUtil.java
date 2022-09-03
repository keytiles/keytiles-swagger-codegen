package com.keytiles.swagger.codegen.helper;

import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.error.SchemaValidationException;
import com.keytiles.swagger.codegen.helper.debug.ModelInlineMessages;
import com.keytiles.swagger.codegen.helper.debug.ModelMessageType;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;

/**
 * Static methods collection to handle common tasks around Codegen models, properties and
 * postProcess* hooks
 *
 * @author attilaw
 *
 */
public class CodegenUtil {

	public final static String ALLOWEDVALUES_KEY_VALUES = "values";
	public final static String ALLOWEDVALUES_KEY_ENUMVARS = "enumVars";

	private CodegenUtil() {
	}

	public static boolean isModelDirectlyDeclaredInSchema(CodegenModel theModel) {
		return StringUtils.isNotBlank(theModel.modelJson);
	}

	/**
	 * @return TRUE if this model is a composition ("anyOf", "allOf", "oneOf") of Enums - FALSE
	 *         otherwise
	 */
	public static boolean isModelComposedEnumModel(CodegenModel theModel) {
		if (!theModel.isComposedModel) {
			return false;
		}

		// all subtypes must be enums
		for (CodegenModel subtype : theModel.getSubTypes()) {
			if (!subtype.getIsEnum()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Just some quick and dirty thing to recognize if someone added something like this to the schema:
	 *
	 * <pre>
	 * SomeInlineEnumCompositionWhatever:
	 *   ...
	 *   anyOf/oneOf:
	 *   ...
	 *   - enum:
	 *     - own_error_code_1
	 *     - own_error_code_2
	 *   ...
	 * </pre>
	 *
	 * @param theModel
	 * @return
	 */
	private static boolean isComposedModelUsingInlineEnumDeclaration(CodegenModel theModel) {
		if (theModel.modelJson != null) {

			ObjectMapper objectMapper = new ObjectMapper();
			HashMap<String, Object> modelJson = null;
			Exception parsingException = null;
			try {
				modelJson = objectMapper.readValue(theModel.modelJson, HashMap.class);
			} catch (Exception e) {
				parsingException = e;
			}
			Preconditions.checkState(modelJson != null,
					"Oops it looks we failed to json parse the .modelJson attribute of model '%s'! modelJson\n%s\nlead to error: %s",
					theModel.name, theModel.modelJson, parsingException);

			// let's hunt for anyOf, oneOf things!

			for (Map.Entry<String, Object> entry : modelJson.entrySet()) {
				if ("anyOf".equalsIgnoreCase(entry.getKey()) || "oneOf".equalsIgnoreCase(entry.getKey())
						|| "allOf".equalsIgnoreCase(entry.getKey())) {
					// Ok this is a List basically and inside can be HashMaps
					// but for us for now its enough to check there is no key "enum" in any hashmaps
					if (Objects.toString(entry.getValue()).toLowerCase().contains("enum=")) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * This method is recognizing stuff like this:
	 *
	 * <pre>
	 * ExtendedErrorCodesAnyOf:
	 *   type: string
	 *   anyOf:
	 *   - $ref: "#/components/schemas/LocalErrorCodes"
	 *   - $ref: "imported-types.yaml#/components/schemas/CommonErrorCodes"
	 * </pre>
	 *
	 * @param theModel
	 * @return
	 */
	private static boolean isModelImplementingOneComposedEnumModelInterface(CodegenModel theModel) {
		return theModel.interfaceModels != null && theModel.interfaceModels.size() == 1
				&& isModelComposedEnumModel(theModel.interfaceModels.get(0));
	}

	/**
	 * 'anyOf' and 'oneOf' in Java Codegen is translated the way it will create interfaces. And classes
	 * (models) who are participating
	 *
	 * @param theModel
	 * @return
	 */
	public static boolean isModelImplementingAnyComposedEnumModelInterfaces(CodegenModel theModel) {
		if (theModel.interfaceModels == null) {
			return false;
		}
		for (CodegenModel interfaceModel : theModel.interfaceModels) {
			if (isModelComposedEnumModel(interfaceModel)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * If the given model is "anyOf", "allOf", "oneOf" composed model where all referenced parts are
	 * Enums then it returns a new {@link CodegenModel} with same name everything but it will be
	 * converted to a merged Enum of all parts
	 *
	 * @param theComposedEnumModelCandidate
	 *            the input model assumed to be a composed Enum model
	 * @return a merged Enum model or NULL if input model was not a composed Enum model
	 */
	public static CodegenModel getComposedEnumModelAsMergedEnumModel(CodegenModel theComposedEnumModelCandidate,
			boolean addExplanationsToModel) throws IllegalStateException {

		// we can not deal with inline enum compisitions
		if (isComposedModelUsingInlineEnumDeclaration(theComposedEnumModelCandidate)) {
			throw new SchemaValidationException("Oops! We discovered a model in the schema '"
					+ theComposedEnumModelCandidate.name
					+ "' which seems to contain a composition (anyOf, oneOf, ...) which is using inline 'enum' declaration");
		}

		boolean directComposedEnumModel = isModelComposedEnumModel(theComposedEnumModelCandidate);
		boolean indirectComposedEnumModel = isModelImplementingAnyComposedEnumModelInterfaces(
				theComposedEnumModelCandidate);
		boolean isDirectlyDeclared = isModelDirectlyDeclaredInSchema(theComposedEnumModelCandidate);

		// if (directComposedEnumModel || indirectComposedEnumModel) {
		// String str = __stringifyObject(" ", "\n", theComposedEnumModelCandidate);
		// System.out.println("====================== " + theComposedEnumModelCandidate.name + "\n" + str
		// + "\n======================");
		// }

		// the model needs to be either direct or indirect composed enum - if it is not then skipp
		if (!directComposedEnumModel && !indirectComposedEnumModel) {
			return null;
		}
		// AND we also do not want to modify accidentally directly declared Enum models! if thats the case,
		// skip
		if (theComposedEnumModelCandidate.getIsEnum() && isDirectlyDeclared) {
			return null;
		}

		CodegenModel joinedEnumModel = new CodegenModel();
		joinedEnumModel.isComposedModel = false;
		joinedEnumModel.allVars = new ArrayList<>();
		joinedEnumModel.vars = joinedEnumModel.allVars;
		joinedEnumModel.allMandatory = new TreeSet<>();
		joinedEnumModel.mandatory = joinedEnumModel.allMandatory;
		joinedEnumModel.classFilename = theComposedEnumModelCandidate.classFilename;
		joinedEnumModel.classname = theComposedEnumModelCandidate.classname;
		joinedEnumModel.name = theComposedEnumModelCandidate.name;
		joinedEnumModel.classVarName = theComposedEnumModelCandidate.classVarName;
		joinedEnumModel.imports = theComposedEnumModelCandidate.imports;
		joinedEnumModel.interfaces = new ArrayList<>();
		joinedEnumModel.parentVars = new ArrayList<>();
		joinedEnumModel.readOnlyVars = new ArrayList<>();
		joinedEnumModel.readWriteVars = new ArrayList<>();
		joinedEnumModel.requiredVars = new ArrayList<>();
		joinedEnumModel.optionalVars = new ArrayList<>();
		joinedEnumModel.vendorExtensions = new HashMap<>();
		joinedEnumModel.vendorExtensions.put(CodegenConstants.IS_ENUM_EXT_NAME, true);

		joinedEnumModel.allowableValues = new HashMap<>();
		List<Object> values = new ArrayList<>();
		List<Object> enumVars = new ArrayList<>();
		joinedEnumModel.allowableValues.put(ALLOWEDVALUES_KEY_VALUES, values);
		joinedEnumModel.allowableValues.put(ALLOWEDVALUES_KEY_ENUMVARS, enumVars);

		List<CodegenModel> subtypes = theComposedEnumModelCandidate.getSubTypes();
		if (indirectComposedEnumModel) {
			subtypes = theComposedEnumModelCandidate.interfaceModels.get(0).subTypes;
		}

		// let's iterate over the subtypes (which are enums) and merge them!
		List<String> subtypeNames = new ArrayList<>(subtypes.size());
		for (CodegenModel subEnum : subtypes) {
			if (joinedEnumModel.dataType == null) {
				// inherit the data type
				joinedEnumModel.dataType = subEnum.dataType;
			} else {
				if (!joinedEnumModel.dataType.equals(subEnum.dataType)) {
					throw new SchemaValidationException("At model '" + theComposedEnumModelCandidate.name
							+ "': Composition of enums - the datatype of composed enums are not the same however they should be! Pleace check!");
				}
			}

			values.addAll((List<?>) subEnum.allowableValues.get("values"));
			enumVars.addAll((List<?>) subEnum.allowableValues.get("enumVars"));

			subtypeNames.add(subEnum.classname);
		}

		// let's do a validation - there should not be any overlap btw the merged enums (at least for now go
		// clean!)
		Set<Object> valuesSet = new HashSet<>(values);
		Set<Object> enumVarsSet = new HashSet<>(enumVars);
		if (valuesSet.size() != values.size() || enumVarsSet.size() != enumVars.size()) {
			throw new SchemaValidationException("At model '" + theComposedEnumModelCandidate.name
					+ "': enums to be composed have overlap! This is not supported for now please ensure Enums to be composed have no intersection!");
		}

		// let's mark this model so we can know later we altered this here
		joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_MERGED_ENUM, true);
		if (isDirectlyDeclared) {
			joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_SCHEMA_DEFINED_MERGED_ENUM, true);
		} else {
			joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_FABRICATED_MERGED_ENUM, true);
		}

		// let's hook in the explanations if feature is requested
		if (addExplanationsToModel) {
			// we will simply create instances on every model and property
			ModelInlineMessages.getOrCreateMessages(joinedEnumModel, ModelMessageType.EXPLANATION);
			// let's add some explanation!
			StringBuilder explanation = new StringBuilder("Keytiles Codegen generated this as merged Enum");
			if (isDirectlyDeclared) {
				explanation.append(" - came from '").append(theComposedEnumModelCandidate.classname)
						.append("' schema model");
			} else {
				explanation.append(" - fabricated by Codegen so not something you declared");
			}
			explanation.append(", composed from: ");
			explanation.append(Joiner.on(", ").join(subtypeNames));
			ModelInlineMessages.appendToClass(joinedEnumModel, ModelMessageType.EXPLANATION, explanation.toString());
		}

		return joinedEnumModel;

	}

	/**
	 * Checking two Enums and tells if they are basically equal to each other or not
	 *
	 * @return TRUE if both models are Enums and they encode the same values - false otherwise
	 */
	public static boolean areEnumModelsEqual(CodegenModel model1, CodegenModel model2) {
		if (!model1.getIsEnum() || !model2.getIsEnum()) {
			return false;
		}

		Set<Object> allowedValues1 = new HashSet<>((List<?>) model1.allowableValues.get(ALLOWEDVALUES_KEY_VALUES));
		Set<Object> allowedValues2 = new HashSet<>((List<?>) model2.allowableValues.get(ALLOWEDVALUES_KEY_VALUES));
		Set<Object> allowedValuesEnumVars1 = new HashSet<>(
				(List<?>) model1.allowableValues.get(ALLOWEDVALUES_KEY_ENUMVARS));
		Set<Object> allowedValuesEnumVars2 = new HashSet<>(
				(List<?>) model2.allowableValues.get(ALLOWEDVALUES_KEY_ENUMVARS));

		return allowedValues1.equals(allowedValues2) && allowedValuesEnumVars1.equals(allowedValuesEnumVars2);
	}

	/**
	 * AttilaW: The presence of this method is definitely a hack... We really should not manipulate
	 * imports here but could not find better way now...
	 *
	 * Helpful in {@link AbstractJavaCodegen#postProcessAllModels(Map)} hooks. The method adds a class
	 * to the imports
	 *
	 * @param postProcessModelEntry
	 *            an {@link Entry} you grabbed from the Map input of this hook
	 * @param classNameToBeImported
	 *            which class to add to the imports? note: if this is NULL then method simply returns
	 *            without doing anything
	 *
	 */
	@SuppressWarnings("unchecked")
	public static void addImportToModelMapOnPostProcessAllModelsHook(Map.Entry<String, Object> postProcessModelEntry,
			String classNameToBeImported) {
		if (classNameToBeImported == null) {
			// simply skip
			return;
		}

		Map<String, Object> modelMap = (Map<String, Object>) postProcessModelEntry.getValue();
		List<Map<String, Object>> imports = (List<Map<String, Object>>) modelMap.get("imports");

		// let's scan them through and add only if not added yet!
		for (Map<String, Object> importItem : imports) {
			if (classNameToBeImported.equals(importItem.get("import"))) {
				// already added - skip
				return;
			}
		}

		// let's add!
		Map<String, Object> stupidImportMap = new HashMap<>();
		stupidImportMap.put("import", classNameToBeImported);
		imports.add(stupidImportMap);
	}

	/**
	 * Wrapper with different parametrization around
	 * {@link #addImportToModelMapOnPostProcessAllModelsHook(java.util.Map.Entry, String)}
	 *
	 * @param postProcessInputMap
	 *            the Map input of this hook
	 * @param modelClassName
	 *            which model class you are interested in?
	 */
	public static void addImportToModelMapOnPostProcessAllModelsHook(Map<String, Object> postProcessInputMap,
			String modelClassName, String classNameToBeImported) {

		Object modelEntry = postProcessInputMap.get(modelClassName);
		Preconditions.checkArgument(modelEntry != null, "Could not find model named '%s' in modelMap", modelClassName);
		addImportToModelMapOnPostProcessAllModelsHook(new SimpleEntry<String, Object>(modelClassName, modelEntry),
				classNameToBeImported);
	}

	/**
	 * Helpful in {@link AbstractJavaCodegen#postProcessAllModels(Map)} hooks.
	 *
	 * @param postProcessModelEntry
	 *            an {@link Entry} you grabbed from the Map input of this hook
	 * @return the {@link CodegenModel} extracted from that entry
	 */
	public static CodegenModel extractModelClassFromPostProcessAllModelsInput(
			Map.Entry<String, Object> postProcessModelEntry) {
		Map<String, Object> modelMap = (Map<String, Object>) postProcessModelEntry.getValue();
		List<Map<String, Object>> models = (List<Map<String, Object>>) modelMap.get(CodegenConstants.MODELS);
		Preconditions.checkState(models.size() == 1,
				"Oops! It looks model '%s' has more (sub)model entries than expected exact 1",
				postProcessModelEntry.getKey());
		return (CodegenModel) models.get(0).get("model");
	}

	/**
	 * Wrapper around {@link #extractModelClassFromPostProcessAllModelsInput(java.util.Map.Entry)} but
	 * here you pass in different params
	 *
	 * @param postProcessInputMap
	 *            the Map input of this hook
	 * @param modelClassName
	 *            which model class you are interested in?
	 * @return the {@link CodegenModel} extracted from that entry or NULL if the model class is not
	 *         found in the map
	 */
	public static CodegenModel extractModelClassFromPostProcessAllModelsInput(Map<String, Object> postProcessInputMap,
			String modelClassName) {
		Object modelEntry = postProcessInputMap.get(modelClassName);
		if (modelEntry == null) {
			return null;
		}
		return extractModelClassFromPostProcessAllModelsInput(new SimpleEntry<>(modelClassName, modelEntry));
	}

	public static void replaceModelInPostProcessAllModelsInput(Map<String, Object> postProcessInputMap,
			String modelClassName, CodegenModel withModel) {
		Map<String, Object> origModelMap = (Map<String, Object>) postProcessInputMap.get(modelClassName);

		// inputSpec?
		// imports

		List<Map<String, Object>> models = (List<Map<String, Object>>) origModelMap.get(CodegenConstants.MODELS);
		Preconditions.checkState(models.size() == 1,
				"Oops! It looks model '%s' has more (sub)model entries than expected exact 1", modelClassName);
		models.get(0).put("model", withModel);

		if (withModel.getIsEnum()) {

		}
	}

	/**
	 * just for debugging nothing else
	 */
	public static String __stringifyObject(String indent, String newLine, Object object) {
		StringBuffer sb = new StringBuffer();
		sb.append(indent).append(object.getClass().getName()).append(newLine);
		for (Field field : object.getClass().getDeclaredFields()) {
			field.setAccessible(true);
			String name = field.getName();
			Object value;
			try {
				value = field.get(object);
			} catch (Exception e) {
				value = "!! failed !! - error: " + e.getMessage();
			}
			sb.append(indent).append(name).append(": ").append(value).append(newLine);
		}
		return sb.toString();
	}

	/**
	 * just for debugging nothing else
	 */
	public static void __dumpModelMapMapToSysOut(Map<String, Object> modelMap) {
		modelMap.entrySet().forEach(entry -> {
			if (entry.getKey().equals("inputSpec")) {
				// skip this - its big
			} else if (entry.getKey().equals("models")) {
				List<Object> models = (List<Object>) entry.getValue();
				System.out.println("   - " + entry.getKey() + "[0]:");

				Map<String, Object> modelsEntryMap = (Map<String, Object>) models.get(0);
				modelsEntryMap.entrySet().forEach(modelsEntry -> {
					if (modelsEntry.getValue() instanceof CodegenModel) {
						String objString = __stringifyObject("", " || ", modelsEntry.getValue());
						System.out.println("      - " + modelsEntry.getKey() + ": " + objString);
					} else {
						System.out.println("      - " + modelsEntry.getKey() + ": " + modelsEntry.getValue());
					}
				});
			} else {
				System.out.println("   - " + entry.getKey() + ": " + entry.getValue());
			}
		});
	}

}
