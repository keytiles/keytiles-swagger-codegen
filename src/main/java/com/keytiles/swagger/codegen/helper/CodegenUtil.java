package com.keytiles.swagger.codegen.helper;

import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.IKeytilesCodegen.ModelState;
import com.keytiles.swagger.codegen.error.SchemaValidationException;
import com.keytiles.swagger.codegen.helper.debug.ModelInlineMessages;
import com.keytiles.swagger.codegen.helper.debug.ModelMessageType;
import com.keytiles.swagger.codegen.helper.debug.PropertyInlineMessages;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;

/**
 * Static methods collection to handle common tasks around Codegen models, properties and
 * postProcess* hooks
 *
 * @author attilaw
 *
 */
public class CodegenUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(CodegenUtil.class);

	public final static String ALLOWEDVALUES_KEY_VALUES = "values";
	public final static String ALLOWEDVALUES_KEY_ENUMVARS = "enumVars";

	private CodegenUtil() {
	}

	/**
	 * Checks if the given model is at least on this state
	 *
	 * @param model
	 * @param atLeastThisState
	 */
	public static void validateModelState(CodegenModel model, ModelState atLeastThisState) {
		ModelState modelState = (ModelState) model.vendorExtensions.get(IKeytilesCodegen.X_MODEL_STATE);
		if (modelState == null) {
			throw new IllegalStateException(
					"Oops! Something is wrong with model '" + model + "'... The state stored in '"
							+ IKeytilesCodegen.X_MODEL_STATE + "' is not present... This is definitely codegen bug!");
		}
		if (modelState.getLevel() < atLeastThisState.getLevel()) {
			throw new IllegalStateException("Oops! You try to do something too early! The model '" + model
					+ "' is not in the expected '" + atLeastThisState + "' state yet but just in '" + modelState
					+ "' state! This is definitely codegen bug!");
		}
	}

	/**
	 * Parses the .modelJson into a hashmap if not null - otherwise an empty map is returned
	 *
	 * @param theModel
	 * @return the parsed .modelJson
	 * @throws IllegalStateException
	 *             in case parsing has hard-failed
	 */
	public static HashMap<String, Object> getParsedModelJson(CodegenModel theModel) {
		HashMap<String, Object> modelJson = null;
		if (theModel.modelJson != null) {

			ObjectMapper objectMapper = new ObjectMapper();
			Exception parsingException = null;
			try {
				modelJson = objectMapper.readValue(theModel.modelJson, HashMap.class);
			} catch (Exception e) {
				parsingException = e;
			}
			Preconditions.checkState(modelJson != null,
					"Oops it looks we failed to json parse the .modelJson attribute of model '%s'! modelJson\n%s\nlead to error: %s",
					theModel.name, theModel.modelJson, parsingException);
		} else {
			modelJson = new HashMap<>();
		}

		return modelJson;
	}

	/**
	 * Scans the given collection of properties searching for a property by its name.
	 *
	 * @param properties
	 *            the collection of props
	 * @param propertyName
	 *            the name of the prop we are searching for
	 * @return the property if it is found in the collection - NULL if not
	 */
	public static CodegenProperty getPropertyByName(Collection<CodegenProperty> properties, String propertyName) {
		Preconditions.checkArgument(properties != null, "'properties' argument was NULL which is invalid here");
		Preconditions.checkArgument(propertyName != null, "'propertyName' argument was NULL which is invalid here");

		for (CodegenProperty prop : properties) {
			if (prop.name.equals(propertyName)) {
				return prop;
			}
		}
		return null;
	}

	/**
	 * Scans the given collection of properties searching for a property by its baseName. This is the
	 * name it is defined in the schema.
	 *
	 * @param properties
	 *            the collection of props
	 * @param propertyBaseName
	 *            the name of the prop in the schema we are searching for
	 * @return the property if it is found in the collection - NULL if not
	 */
	public static CodegenProperty getPropertyByBaseName(Collection<CodegenProperty> properties,
			String propertyBaseName) {
		Preconditions.checkArgument(properties != null, "'properties' argument was NULL which is invalid here");
		Preconditions.checkArgument(propertyBaseName != null,
				"'propertyBaseName' argument was NULL which is invalid here");

		for (CodegenProperty prop : properties) {
			if (prop.baseName.equals(propertyBaseName)) {
				return prop;
			}
		}
		return null;
	}

	/**
	 * @return the property with the given name from the model - NULL if there is no property with this
	 *         name
	 */
	public static CodegenProperty getPropertyByName(CodegenModel model, String propertyName) {
		Preconditions.checkArgument(model != null, "'model' argument was NULL which is invalid here");
		return getPropertyByName(model.vars, propertyName);
	}

	/**
	 * @return the property with the given schema-name (baseName) from the model - NULL if there is no
	 *         property with this name
	 */
	public static CodegenProperty getPropertyByBaseName(CodegenModel model, String propertyBaseName) {
		Preconditions.checkArgument(model != null, "'model' argument was NULL which is invalid here");
		return getPropertyByBaseName(model.vars, propertyBaseName);
	}

	/**
	 * Takes a list of properties and if there is property in it with the given name then it will be
	 * replaced with the given property at the same place. So the order of properties in the returned
	 * list will be the same.
	 * <p>
	 * note: the assumption is that there is just one property (max) in the list with the given name. If
	 * another one is found the method will fail
	 *
	 * @param properties
	 *            the list to scan through
	 * @param propertyName
	 *            the name of the property to be replaced
	 * @param replaceWith
	 *            the property to replace with - if NULL then the property will be simply removed
	 * @return a new list of properties in which the replacement is done
	 * @throws IllegalArgumentException
	 *             if property with the given name is found more than once in the list
	 */
	public static List<CodegenProperty> replacePropertyByName(List<CodegenProperty> properties, String propertyName,
			CodegenProperty replaceWith) throws IllegalArgumentException {
		Preconditions.checkArgument(properties != null, "'properties' argument was NULL which is invalid here");
		Preconditions.checkArgument(propertyName != null, "'propertyName' argument was NULL which is invalid here");

		List<CodegenProperty> result = new ArrayList<>(properties.size());

		boolean oneFound = false;
		for (CodegenProperty prop : properties) {
			if (prop.name.equals(propertyName)) {
				// only one replacement is allowed
				Preconditions.checkArgument(oneFound == false,
						"There are multiple properties with name '%s' in the collection - this is against the expectations!",
						propertyName);
				oneFound = true;

				if (replaceWith != null) {
					result.add(replaceWith);
				}
			} else {
				result.add(prop);
			}
		}
		return result;
	}

	/**
	 * Takes a list of properties and if there is property in it with the given schema-name (baseName)
	 * then it will be replaced with the given property at the same place. So the order of properties in
	 * the returned list will be the same.
	 * <p>
	 * note: the assumption is that there is just one property (max) in the list with the given name. If
	 * another one is found the method will fail
	 *
	 * @param properties
	 *            the list to scan through
	 * @param propertyBaseName
	 *            the schema-name of the property to be replaced
	 * @param replaceWith
	 *            the property to replace with - if NULL then the property will be simply removed
	 * @return a new list of properties in which the replacement is done
	 * @throws IllegalArgumentException
	 *             if property with the given name is found more than once in the list
	 */
	public static List<CodegenProperty> replacePropertyByBaseName(List<CodegenProperty> properties,
			String propertyBaseName, @Nullable CodegenProperty replaceWith) throws IllegalArgumentException {
		Preconditions.checkArgument(properties != null, "'properties' argument was NULL which is invalid here");
		Preconditions.checkArgument(propertyBaseName != null,
				"'propertyBaseName' argument was NULL which is invalid here");

		List<CodegenProperty> result = new ArrayList<>(properties.size());

		boolean oneFound = false;
		for (CodegenProperty prop : properties) {
			if (prop.baseName.equals(propertyBaseName)) {
				// only one replacement is allowed
				Preconditions.checkArgument(oneFound == false,
						"There are multiple properties with baseName '%s' in the collection - this is against the expectations!",
						propertyBaseName);
				oneFound = true;

				if (replaceWith != null) {
					result.add(replaceWith);
				}
			} else {
				result.add(prop);
			}
		}
		return result;
	}

	/**
	 * Comfort wrapper around {@link #replacePropertyByName(List, String, CodegenProperty)} - this one
	 * takes not a list but a set (maintained order)
	 */
	public static LinkedHashSet<CodegenProperty> replacePropertyByName(Set<CodegenProperty> properties,
			String propertyName, @Nullable CodegenProperty replaceWith) throws IllegalArgumentException {
		Preconditions.checkArgument(properties != null, "'properties' argument was NULL which is invalid here");

		List<CodegenProperty> list = new ArrayList<>(properties);
		LinkedHashSet<CodegenProperty> result = new LinkedHashSet<>(
				replacePropertyByName(list, propertyName, replaceWith));
		return result;
	}

	/**
	 * Comfort wrapper around {@link #replacePropertyByBaseName(List, String, CodegenProperty)} - this
	 * one takes not a list but a set (maintained order)
	 */
	public static LinkedHashSet<CodegenProperty> replacePropertyByBaseName(Set<CodegenProperty> properties,
			String propertyBaseName, @Nullable CodegenProperty replaceWith) throws IllegalArgumentException {
		Preconditions.checkArgument(properties != null, "'properties' argument was NULL which is invalid here");

		List<CodegenProperty> list = new ArrayList<>(properties);
		LinkedHashSet<CodegenProperty> result = new LinkedHashSet<>(
				replacePropertyByBaseName(list, propertyBaseName, replaceWith));
		return result;
	}

	/**
	 * Removes a property with the given name from the collection. This method is changing the
	 * collection. If you want the param collection unchanged, and prefer to get back a copy instead,
	 * see {@link #replacePropertyByName(LinkedHashSet, String, CodegenProperty)} or
	 * {@link #replacePropertyByName(List, String, CodegenProperty)} invoking with replaceWith=NULL!
	 *
	 * @param properties
	 *            the collection of props
	 * @param propertyName
	 *            name of the prop to remove
	 * @return the property which was removed or NULL if there was no prop with this name
	 */
	public static CodegenProperty removePropertyByName(Collection<CodegenProperty> properties, String propertyName)
			throws IllegalArgumentException {
		CodegenProperty prop = getPropertyByName(properties, propertyName);
		if (prop != null) {
			properties.remove(prop);
		}
		return prop;
	}

	/**
	 * Removes a property with the given schema-name (baseName) from the collection. This method is
	 * changing the collection. If you want the param collection unchanged, and prefer to get back a
	 * copy instead, see {@link #replacePropertyByBaseName(LinkedHashSet, String, CodegenProperty)} or
	 * {@link #replacePropertyByBaseName(List, String, CodegenProperty)} invoking with replaceWith=NULL!
	 *
	 * @param properties
	 *            the collection of props
	 * @param propertyBaseName
	 *            name of the prop to remove
	 * @return the property which was removed or NULL if there was no prop with this name
	 */
	public static CodegenProperty removePropertyByBaseName(Collection<CodegenProperty> properties,
			String propertyBaseName) throws IllegalArgumentException {
		CodegenProperty prop = getPropertyByBaseName(properties, propertyBaseName);
		if (prop != null) {
			properties.remove(prop);
		}
		return prop;
	}

	/**
	 * Tells if a model is extending another one (or equal to it) or not
	 *
	 * @param model
	 *            the model you are curious about - can not be NULL!
	 * @param assignableFromModel
	 *            the model which you think might be the superclass - can not be NULL!
	 * @return TRUE if "model" extends "superclassCandidate" - FALSE otherwise
	 */
	public static boolean isModelAssignableFromModel(CodegenModel model, CodegenModel assignableFromModel) {
		Preconditions.checkArgument(model != null, "'model' argument was NULL which is invalid here");
		Preconditions.checkArgument(assignableFromModel != null,
				"'assignableFromModel' argument was NULL which is invalid here");
		validateModelState(model, ModelState.baseCodegenFullyEnriched);

		// equality?
		if (model.name.equals(assignableFromModel.name)) {
			return true;
		}

		// extending?
		CodegenModel parent = assignableFromModel.parentModel;
		while (parent != null) {
			if (parent.name.equals(model.name)) {
				// we are good
				return true;
			}
			parent = parent.parentModel;
		}
		return false;
	}

	/**
	 * Tells if a property (of a model) is assignable from another property (same or another model) or
	 * not.
	 * <p>
	 * This basically means either
	 * <ul>
	 * <li>both using primitive datatype and these datatypes are the same, or
	 * <li>the property has an object datatype and the other property is either the same object or an
	 * object which extends the object, or
	 * <li>both are arrays or maps - and the items are assignable from each other due to the above
	 * </ul>
	 *
	 * @param property
	 *            the property you would like to put on the left side of the assignment (gets the value)
	 * @param assignableFromProperty
	 *            the property you would like to pput to the right side of the assignment (it will be
	 *            the value to assign)
	 * @param allModels
	 *            collection of all data models which are in the generation context (maybe see
	 *            {@link IKeytilesCodegen#getAllModels()}
	 * @return TRUE if the property = assignableFromProperty assignment would work - FALSE otherwise
	 */
	public static boolean isPropertyAssignableFromProperty(CodegenProperty property,
			CodegenProperty assignableFromProperty, Collection<CodegenModel> allModels) {
		Preconditions.checkArgument(property != null, "'property' argument was NULL which is invalid here");
		Preconditions.checkArgument(assignableFromProperty != null,
				"'assignFromProperty' argument was NULL which is invalid here");

		// if this is an array...
		if (property.getIsArrayModel()) {
			if (assignableFromProperty.getIsArrayModel()) {
				// ... and the this one too then Item should decide
				return isPropertyAssignableFromProperty(property.items, assignableFromProperty.items, allModels);
			} else {
				// ... otherwise.. no
				return false;
			}
		}

		// if this is a list...
		if (property.getIsListContainer()) {
			if (assignableFromProperty.getIsListContainer()) {
				// ... and the this one too then Item should decide
				return isPropertyAssignableFromProperty(property.items, assignableFromProperty.items, allModels);
			} else {
				// ... otherwise.. no
				return false;
			}
		}

		// if this is a map...
		if (property.getIsMapContainer()) {
			if (assignableFromProperty.getIsMapContainer()) {
				// ... and the this one too then Item should decide
				return isPropertyAssignableFromProperty(property.items, assignableFromProperty.items, allModels);
			} else {
				// ... otherwise.. no
				return false;
			}
		}

		// so far we finished with "container" cases

		// let's make a fail safe!
		Preconditions.checkState(property.items == null,
				"Oops! It looks property '%s' has items... but code is not prepared for this! Please report this case as a bug!",
				property);

		if (assignableFromProperty.getIsListContainer() || assignableFromProperty.getIsArrayModel()
				|| assignableFromProperty.getIsMapContainer()) {
			return false;
		}
		// another failsafe
		Preconditions.checkState(assignableFromProperty.items == null,
				"Oops! It looks property '%s' has items... but code is not prepared for this! Please report this case as a bug!",
				assignableFromProperty);

		// ok so property if not a array, not map - it can be primitive or an object
		// but the point is: it is in the .datatype attrib

		// trivial case
		if (Objects.equals(property.datatype, assignableFromProperty.datatype)) {
			return true;
		}

		// let's find the model the properties are taking
		// note: it is possible (if primitive type e.g.) we will not find them
		CodegenModel propertyModel = null;
		CodegenModel assignFromPropertyModel = null;
		for (CodegenModel model : allModels) {
			if (model.name.equals(property.datatype)) {
				propertyModel = model;
			}
			if (model.name.equals(assignableFromProperty.datatype)) {
				assignFromPropertyModel = model;
			}
		}

		// for several reasons it is possible a datatype is not found in models
		// it can be a primitive type OR maybe "BigDecimal" or "Float" is assigned
		// the point is: if we do not find ANY model then straight equality should be used for now
		// but we actually already checked that ^^^ see above!
		// so... no...
		if (propertyModel == null || assignFromPropertyModel == null) {
			return false;
		}

		// so we have 2 models
		// good! then...
		return isModelAssignableFromModel(propertyModel, assignFromPropertyModel);
	}

	/**
	 * Tells if a model is directly declared in the schema (Any schema... Even schemas which is not the
	 * current one (we are generating) but we imported the model from another schema using $ref!) by the
	 * user, or just "fabricated" by the Codegen because it was declared inline or similar situation has
	 * happened
	 *
	 * @return TRUE if this model is fabricated by codegen - FALSE if this is declared by the user in
	 *         schema
	 */
	public static boolean isModelFabricatedModel(CodegenModel theModel) {
		boolean isFabriacted = StringUtils.isBlank(theModel.modelJson);
		// if (isFabriacted) {
		// LOGGER.info("buu");
		// }
		return isFabriacted;
	}

	/**
	 * Wrapper around {@link CodegenBugfixAndEnhanceHelper#isOwnModel(CodegenModel)} - see description
	 * there
	 */
	public static boolean isOwnModel(CodegenModel model) {
		return CodegenBugfixAndEnhanceHelper.isOwnModel(model);
	}

	/**
	 * Checks if the user assigned default value to the property or not
	 */
	public static boolean hasPropertyUserAssignedDefaultValue(CodegenModel model, CodegenProperty property) {
		if (property.jsonSchema != null) {
			return property.jsonSchema.contains("\"default\"");
		}
		return false;
	}

	/**
	 * Checks if the property has a default value or not
	 *
	 */
	public static boolean hasPropertyDefaultValue(CodegenModel model, CodegenProperty property) {
		return property.defaultValue != null && !"null".equals(property.defaultValue);
	}

	/**
	 * Checks if a property is mandatory in the model
	 */
	public static boolean isPropertyMandatory(CodegenModel model, CodegenProperty property) {
		return model.mandatory.contains(property.baseName);
	}

	/**
	 * @return TRUE if this model is a composition ("anyOf", "allOf", "oneOf") of Enums - FALSE
	 *         otherwise
	 */
	public static boolean isModelComposedEnumModel(CodegenModel theModel) {
		if (!CodegenBugfixAndEnhanceHelper.isComposedModel(theModel)) {
			return false;
		}

		if (theModel.getSubTypes() == null) {
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
		HashMap<String, Object> modelJson = getParsedModelJson(theModel);
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
		boolean isDirectlyDeclared = !isModelFabricatedModel(theComposedEnumModelCandidate);

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
		// let's inherit this
		joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_MODEL_IS_OWN_MODEL,
				CodegenBugfixAndEnhanceHelper.isOwnModel(theComposedEnumModelCandidate));
		// this fabricated model will not be more "enriched" for sure :-)
		joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_MODEL_STATE, ModelState.fullyEnriched);

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
		joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_MODEL_MERGED_ENUM, true);
		if (isDirectlyDeclared) {
			joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_MODEL_SCHEMA_DEFINED_MERGED_ENUM, true);
		} else {
			joinedEnumModel.vendorExtensions.put(IKeytilesCodegen.X_MODEL_FABRICATED_MERGED_ENUM, true);
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

	/**
	 * This method is replacing a model definition (in place) with another one which you pass in. So the
	 * name of the model is not changed - only the content (well, the definition) is replaced. You can
	 * invoke this method from {@link AbstractJavaCodegen#postProcessAllModels(Map)} hooks.
	 *
	 * @param postProcessInputMap
	 *            Input from {@link AbstractJavaCodegen#postProcessAllModels(Map)} hook
	 * @param modelClassNameToReplace
	 *            The existing model which model class you want to replace
	 * @param withModel
	 *            With which model
	 */
	public static void replaceModelDefinitionInPostProcessAllModelsInput(Map<String, Object> postProcessInputMap,
			String modelClassNameToReplace, CodegenModel withModel) {
		Map<String, Object> origModelMap = (Map<String, Object>) postProcessInputMap.get(modelClassNameToReplace);
		Preconditions.checkArgument(origModelMap != null,
				"Oops! It looks model '%s' does not exist - replacing of a non-existing model is not possible",
				modelClassNameToReplace);

		// inputSpec?
		// imports

		List<Map<String, Object>> models = (List<Map<String, Object>>) origModelMap.get(CodegenConstants.MODELS);
		Preconditions.checkState(models.size() == 1,
				"Oops! It looks model '%s' has more (sub)model entries than expected exact 1", modelClassNameToReplace);
		models.get(0).put("model", withModel);

		if (withModel.getIsEnum()) {

		}
	}

	private static boolean replaceTypeReferencesInProperty(CodegenProperty property, String modelClassNameToReplace,
			String modelClassNameToReplaceWith) {
		boolean wasActioned = false;

		if (modelClassNameToReplace.equals(property.baseType)) {
			// this is a direct match
			property.baseType = modelClassNameToReplaceWith;
			wasActioned = true;
		}
		if (modelClassNameToReplace.equals(property.complexType)) {
			property.complexType = modelClassNameToReplaceWith;
			wasActioned = true;
		}

		if (property.datatype != null && property.datatype.contains(modelClassNameToReplace)) {
			property.datatype = property.datatype.replace(modelClassNameToReplace, modelClassNameToReplaceWith);
			wasActioned = true;
		}

		if (property.datatypeWithEnum != null && property.datatypeWithEnum.contains(modelClassNameToReplace)) {
			property.datatypeWithEnum = property.datatypeWithEnum.replace(modelClassNameToReplace,
					modelClassNameToReplaceWith);
			wasActioned = true;
		}

		return wasActioned;
	}

	/**
	 * This method can be used if you want to replace references of a model with another. You can invoke
	 * this method from {@link AbstractJavaCodegen#postProcessAllModels(Map)} hooks.
	 * <p>
	 * The method scans through all models and all of their properties and whichever property is using
	 * modelClassNameToReplace will be repointed to use modelClassNameToReplaceWith instead. This means
	 * that after this is done you can even remove the modelClassNameToReplace model from the
	 * postProcessInputMap map if you like
	 *
	 * @param postProcessInputMap
	 *            the input of the {@link AbstractJavaCodegen#postProcessAllModels(Map)} method
	 * @param modelClassNameToReplace
	 *            name of the model which references you want to be replaced
	 * @param modelClassNameToReplaceWith
	 *            with which model reference (name)
	 * @param replacingReason
	 *            explanation of why this replacement was done? this will be added as a Generator
	 *            comment message to the property to create visibility
	 */
	public static void replaceModelReferenceInPostProcessAllModelsInput(Map<String, Object> postProcessInputMap,
			String modelClassNameToReplace, String modelClassNameToReplaceWith, String replacingReason) {

		postProcessInputMap.entrySet().forEach(modelEntry -> {
			CodegenModel theModel = CodegenUtil.extractModelClassFromPostProcessAllModelsInput(modelEntry);

			// if ("ErrorResponseClass".equals(theModel.name)) {
			// LOGGER.info("buu");
			// }

			for (CodegenProperty property : theModel.allVars) {
				boolean wasActioned = replaceTypeReferencesInProperty(property, modelClassNameToReplace,
						modelClassNameToReplaceWith);

				if (property.items != null) {
					replaceTypeReferencesInProperty(property.items, modelClassNameToReplace,
							modelClassNameToReplaceWith);
				}

				// let's make the change visible!
				if (wasActioned) {
					PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION, replacingReason);
				}
			}

		});
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
