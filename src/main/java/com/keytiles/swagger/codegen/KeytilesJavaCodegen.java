package com.keytiles.swagger.codegen;

import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.keytiles.swagger.codegen.error.SchemaValidationException;
import com.keytiles.swagger.codegen.helper.CodegenBugfixAndEnhanceHelper;
import com.keytiles.swagger.codegen.helper.CodegenUtil;
import com.keytiles.swagger.codegen.helper.config.ConfigOptionHelper;
import com.keytiles.swagger.codegen.helper.config.SchemaParamCollection;
import com.keytiles.swagger.codegen.helper.debug.ModelInlineMessages;
import com.keytiles.swagger.codegen.helper.debug.ModelMessageType;
import com.keytiles.swagger.codegen.helper.debug.PropertyInlineMessages;
import com.keytiles.swagger.codegen.helper.maven.MavenExecutionInfo;
import com.keytiles.swagger.codegen.model.ModelExtraInfo;
import com.keytiles.swagger.codegen.model.ModelStyle;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.DefaultGenerator;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;
import io.swagger.codegen.v3.generators.java.JavaClientCodegen;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Keytiles customized Java code generator
 * <p>
 * This is extending and customizing the default {@link JavaClientCodegen} generator
 *
 * @author attilaw
 *
 */
public class KeytilesJavaCodegen extends JavaClientCodegen implements IKeytilesCodegen {

	private static final Logger LOGGER = LoggerFactory.getLogger(KeytilesJavaCodegen.class);

	private final static String EMPTY_STRING = "";

	public static class OriginalPropertyNames {
		public String name;
		public String getter;
		public String setter;
		public String nameInCamelCase;

		public OriginalPropertyNames(CodegenProperty fromProperty) {
			name = fromProperty.name;
			getter = fromProperty.getter;
			setter = fromProperty.setter;
			nameInCamelCase = fromProperty.nameInCamelCase;
		}

		public void restoreNames(CodegenProperty toProperty) {
			toProperty.name = name;
			toProperty.getter = getter;
			toProperty.setter = setter;
			toProperty.nameInCamelCase = nameInCamelCase;
		}
	}

	public final static String TPLVAR_PUBLIC_FIELDS = "publicFields";
	public final static String TPLVAR_PRIVATE_FINAL_FIELDS = "privateFinalFields";
	public final static String TPLVAR_PRIVATE_FIELDS = "privateFields";
	public final static String TPLVAR_CTOR_NEEDS_CONSTRUCTOR = "needsConstructor";
	public final static String TPLVAR_CTOR_FOR_SUPER_ARGS = "constructorForSuperArgs";
	public final static String TPLVAR_CTOR_PASS_TO_SUPER_ARGS = "constructorPassToSuperArgs";
	public final static String TPLVAR_CTOR_OWN_FIELD_ARGS = "constructorOwnFieldArgs";
	public final static String TPLVAR_CTOR_VALIDATE_NONNULL_VALUE_ARGS = "constructorValidateNonNullArgs";
	public final static String TPLVAR_CTOR_COMBINED_ARGS = "constructorCombinedArgs";

	protected final static Map<String, String> wrapperToPrimitiveTypeMapping;

	static {
		wrapperToPrimitiveTypeMapping = new HashMap<>();
		wrapperToPrimitiveTypeMapping.put("Byte", "byte");
		wrapperToPrimitiveTypeMapping.put("Short", "short");
		wrapperToPrimitiveTypeMapping.put("Integer", "int");
		wrapperToPrimitiveTypeMapping.put("Long", "long");
		wrapperToPrimitiveTypeMapping.put("Float", "float");
		wrapperToPrimitiveTypeMapping.put("Double", "double");
		wrapperToPrimitiveTypeMapping.put("Boolean", "boolean");
	}

	protected ModelStyle modelStyle = ModelStyle.simpleConsistent;
	protected boolean keepPropertyNames = false;
	protected boolean allowRenameConflictingFields = false;
	protected boolean usePrimitiveTypesIfPossible = false;
	protected boolean nullableTagDefaultValue = false;
	protected boolean addExplanationsToModel = false;
	protected boolean excludeImportMappingsFromGeneration = true;
	protected SchemaParamCollection addSchemaModelsToImportMappings = null;

	protected String mavenExecutionId = null;
	protected List<String> addSchemaModelsToImportMappingsFromMavenExecutions = null;

	// these are the name of the models we should not generate but skip
	protected Set<String> excludeModelsFromGeneration;

	protected Map<String, CodegenModel> allModels;

	public KeytilesJavaCodegen() {
		super();
		developerName = DEVELOPER_NAME;
		developerEmail = DEVELOPER_EMAIL;
		developerOrganization = DEVELOPER_ORGANIZATION;
		developerOrganizationUrl = DEVELOPER_ORGANIZATION_URL;

		// we generate all models as Serializable
		setSerializableModel(true);
		// we should not ignore provided import mappings - so let's do this here
		// BUT! please note this will jump back to "true" during processOpts() method...
		// see my comment at discussion:
		// https://github.com/swagger-api/swagger-codegen/issues/10419#issuecomment-1184578892
		setIgnoreImportMapping(false);
		// we use this by default (can be overriden)
		setLibrary("resttemplate");

		CliOption modelStyleOption = CliOption.newString(OPT_MODEL_STYLE,
				"Generate Model classes using this style. Available styles are: " + ModelStyle.values());
		modelStyleOption.addEnum(ModelStyle.inherited.toString(),
				"This is the generation style as coming from underlying Java rendering");
		modelStyleOption.addEnum(ModelStyle.simpleConsistent.toString(),
				"Distinguishing 'required' properties and simplifies model based on that. Non-required fields become public while required fields taken from Constructor and gets a getter.");
		modelStyleOption.setDefault(ModelStyle.inherited.toString());
		cliOptions.add(modelStyleOption);

		CliOption keepNamesOption = CliOption.newBoolean(OPT_KEEP_PROPERTY_NAMES,
				"If true then property names are not altered (to Java style) but kept as it is in all Objects - default is: FALSE");
		keepNamesOption.setDefault("false");
		cliOptions.add(keepNamesOption);

		CliOption allowRenameConflictingFieldsOpption = CliOption.newBoolean(OPT_ALLOW_RENAME_CONFLICTING_FIELDS,
				"If true then Java Codegen 'conflicting property strategy' is allowed to kick in otherwise Codegen will fail the build in case of a detected conflict - default is: FALSE for simpleConsistent style, TRUE for inherited model style");
		allowRenameConflictingFieldsOpption.setDefault("false");
		cliOptions.add(allowRenameConflictingFieldsOpption);

		CliOption usePrimitiveTypesOption = CliOption.newBoolean(OPT_USE_PRIMITIVE_TYPES_IF_POSSIBLE,
				"If true then generated model fields will use Java primitive types instead of wrapper types - whenever it is possible - default is: TRUE");
		usePrimitiveTypesOption.setDefault("false");
		cliOptions.add(usePrimitiveTypesOption);

		/*
		 * mmm not a good idea... let's remove
		 *
		CliOption nullableTagDefaultValueOption = CliOption.newBoolean(OPT_NULLABLE_TAG_DEFAULT_VALUE,
				"Determines what would the generation assume for 'nullable' flag in Properties if the 'nullable' declaration is not given explicitly - default is: FALSE");
		nullableTagDefaultValueOption.setDefault("false");
		cliOptions.add(nullableTagDefaultValueOption);
		 */

		CliOption addExplanationsOption = CliOption.newBoolean(OPT_ADD_EXPLANATIONS_TO_MODEL,
				"Very useful debugging feature! If you set it tue true then generator will add comments to the model fields, methods, constructor and explain why it looks as it does  - default is: FALSE");
		addExplanationsOption.setDefault("false");
		cliOptions.add(addExplanationsOption);

		CliOption excludeImportMappingsFromGenerationOption = CliOption.newBoolean(
				OPT_EXCLUDE_IMPORT_MAPPINGS_FROM_GENERATION,
				"If a model name found in importMappings then that model generation is skipped (as it is imported from somewhere) - default is: TRUE");
		excludeImportMappingsFromGenerationOption.setDefault("true");
		cliOptions.add(excludeImportMappingsFromGenerationOption);

		CliOption addSchemaModelsToImportMappingsOption = CliOption.newString(OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING,
				"Comma separated list of schema specifiers - see README for details! - default is: null");
		addSchemaModelsToImportMappingsOption.setDefault(null);
		cliOptions.add(addSchemaModelsToImportMappingsOption);

		CliOption mavenExecutionIdOption = CliOption.newString(OPT_MAVEN_EXECUTION_ID,
				"Usable only from Maven. This is a unique execution ID - default is: null");
		mavenExecutionIdOption.setDefault(null);
		cliOptions.add(mavenExecutionIdOption);

		CliOption addSchemaModelsToImportMappingsFromMavenExecutionsOption = CliOption.newString(
				OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS,
				"Usable only from Maven. Similar to '" + OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING
						+ "' option but this takes things from specified (comma separated list) previously running (so preceeding) Maven executions - default is: null");
		addSchemaModelsToImportMappingsFromMavenExecutionsOption.setDefault(null);
		cliOptions.add(addSchemaModelsToImportMappingsFromMavenExecutionsOption);

	}

	/**
	 * For some interesting reasons the {@link #processOpts()} is invoked later than the
	 * {@link #getTemplateDir()}... but we need to use some options there already...
	 *
	 * But it looks the {@link #additionalProperties()} are already available on
	 * {@link #getTemplateDir()} too so processing is extracted into a private method and invoked from
	 * everywhere
	 */
	private void processAdditionalOptions() {
		if (additionalProperties.containsKey(OPT_MODEL_STYLE)) {
			modelStyle = ModelStyle.valueOf((String) additionalProperties.get(OPT_MODEL_STYLE));
		}
		if (additionalProperties.containsKey(OPT_KEEP_PROPERTY_NAMES)) {
			keepPropertyNames = Boolean.valueOf(additionalProperties.get(OPT_KEEP_PROPERTY_NAMES).toString());
		}
		if (additionalProperties.containsKey(OPT_ALLOW_RENAME_CONFLICTING_FIELDS)) {
			allowRenameConflictingFields = Boolean
					.valueOf(additionalProperties.get(OPT_KEEP_PROPERTY_NAMES).toString());
		} else {
			// this field has different default - based on modelStyle
			if (modelStyle == ModelStyle.simpleConsistent) {
				allowRenameConflictingFields = false;
			} else {
				allowRenameConflictingFields = true;
			}
		}
		if (additionalProperties.containsKey(OPT_USE_PRIMITIVE_TYPES_IF_POSSIBLE)) {
			usePrimitiveTypesIfPossible = Boolean
					.valueOf(additionalProperties.get(OPT_USE_PRIMITIVE_TYPES_IF_POSSIBLE).toString());
		}
		if (additionalProperties.containsKey(OPT_ADD_EXPLANATIONS_TO_MODEL)) {
			addExplanationsToModel = Boolean
					.valueOf(additionalProperties.get(OPT_ADD_EXPLANATIONS_TO_MODEL).toString());
		}
		if (additionalProperties.containsKey(OPT_EXCLUDE_IMPORT_MAPPINGS_FROM_GENERATION)) {
			excludeImportMappingsFromGeneration = Boolean
					.valueOf(additionalProperties.get(OPT_EXCLUDE_IMPORT_MAPPINGS_FROM_GENERATION).toString());
		}

		if (additionalProperties.containsKey(OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING)) {
			addSchemaModelsToImportMappings = SchemaParamCollection.fromFlatStringDefinition(
					OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING,
					(String) additionalProperties.get(OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING));
		}

		if (additionalProperties.containsKey(OPT_MAVEN_EXECUTION_ID)) {
			mavenExecutionId = (String) additionalProperties.get(OPT_MAVEN_EXECUTION_ID);
		}

		if (additionalProperties.containsKey(OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS)) {
			addSchemaModelsToImportMappingsFromMavenExecutions = Splitter.on(',').trimResults().omitEmptyStrings()
					.splitToList((String) additionalProperties
							.get(OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS));
		}

	}

	@Override
	public String getDefaultTemplateDir() {
		processAdditionalOptions();
		return getName() + File.separator + modelStyle;
	}

	@Override
	public String getName() {
		// note: this drives the default template dir and also the language option you pass to codegen-cli
		// or codegen maven plugin
		return "KeytilesJava";
	}

	@Override
	public String getHelp() {
		return "Generates a Java client library the way we need in Keytiles.";
	}

	@Override
	public void processOpts() {
		super.processOpts();

		// we use Jackson - so for now add some new imports from this lib
		importMapping.put("JsonCreator", "com.fasterxml.jackson.annotation.JsonCreator");
		importMapping.put("JsonInclude", "com.fasterxml.jackson.annotation.JsonInclude");
		importMapping.put("JsonInclude.Include", "com.fasterxml.jackson.annotation.JsonInclude.Include");

		processAdditionalOptions();
	}

	@Override
	public void preprocessOpenAPI(OpenAPI openAPI) {
		super.preprocessOpenAPI(openAPI);

		// let's hook in the magic!
		ConfigOptionHelper.preprocessOpenAPIHook(this, openAPI);
	}

	@Override
	public String apiFilename(String templateName, String tag) {
		return super.apiFilename(templateName, tag);
	}

	/**
	 * Enums are renamed if they have common prefixes - somehow, magically<br>
	 * As reported: https://github.com/swagger-api/swagger-codegen/issues/7725 and
	 * https://github.com/swagger-api/swagger-codegen/issues/5932 they marked as fixed but apparently
	 * not...
	 * <p>
	 * We override this method to enforce turn of prefix crap entirely for now I also filed
	 * https://github.com/swagger-api/swagger-codegen-generators/issues/1075 asking why such a mechanism
	 * exists?
	 */
	@Override
	public String findCommonPrefixOfVars(List<Object> vars) {
		// return super.findCommonPrefixOfVars(vars);
		return EMPTY_STRING;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allSchemas) {
		CodegenModel model = super.fromModel(name, schema, allSchemas);

		// we do not want these annotations being present as import
		model.imports.remove("Schema");
		model.imports.remove("ApiModelProperty");
		model.imports.remove("ApiModel");

		model.vendorExtensions.put(X_MODEL_STATE, ModelState.created);

		// this is an early stage stuff for models which were actually defined in the schema(s)
		// it does not contain the "fabricated" models at this point
		CodegenBugfixAndEnhanceHelper.markOwnModel(model, this);

		// and also let's start to collect models early
		/*
		 * let's dont do this yet - we do it later
		 *
		if (allModels == null) {
			allModels = new HashMap<>();
		}
		allModels.put(model.name, model);
		*/

		return model;
	}

	/**
	 * IMPORTANT NOTE! In this stage the provided {@link CodegenModel} is not complete!
	 * {@link CodegenModel#parentModel} is not yet available! If you need that use one of
	 * {@link #postProcessAllModels(Map)} or {@link #postProcessAllCodegenModels(Map)} hooks
	 *
	 * {@inheritDoc}
	 */
	@Override
	public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
		super.postProcessModelProperty(model, property);

		CodegenBugfixAndEnhanceHelper.validateOnlySupportedVendorAttributesAreUsedOnModelProperty(this, model,
				property);

		// let's hook in the explanations if feature is requested
		if (addExplanationsToModel) {
			// we will simply create instances on every model and property
			ModelInlineMessages.getOrCreateMessages(model, ModelMessageType.EXPLANATION);
			PropertyInlineMessages.getOrCreateMessages(property, ModelMessageType.EXPLANATION);
		}

		if ("StatApiEndpointProblemClass".equals(model.name)) {
			LOGGER.info("buu");
		}

		// if ("arrayFieldWithDefault".equals(property.baseName)
		// || "objectArrayFieldWithDefault".equals(property.baseName)) {
		// LOGGER.info("buu");
		// }

		support_outputOnlyIfNonDefault(model, property);
		support_keepPropertyNames(model, property);

		support_usePrimitiveTypesIfPossible(model, property);

		support_arrayDefaultValue(model, property);
		support_mapDefaultValue(model, property);

	}

	protected void support_mapDefaultValue(CodegenModel model, CodegenProperty property) {
		if (!property.getIsMapContainer()) {
			// we have nothing to do - not an array
			return;
		}
		// does it have a default set in schema?
		if (!CodegenUtil.hasPropertyUserAssignedDefaultValue(model, property)) {

			// OK this array does not have any default items
			// if the property is nullable it does not make sense to create an ArrayList object so lets null it
			// out
			if (property.nullable) {
				property.defaultValue = "null";

				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"this map does not have default and nullable - so let's keep it on NULL then");
			}

			if (CodegenUtil.hasPropertyDefaultValue(model, property)) {
				// just let the user know that Codegen has assigned an empty value to this
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"non-nullable property so Codegen applied a default empty map to it automatically because it is possible with this type");
			}

			return;
		} else {
			throw new SchemaValidationException("The '" + model.name + "." + property.baseName
					+ "' translates into a Map and you added 'default' value to that. This is not supported well so please remove the default value!");
		}
	}

	/**
	 * If the property is an Array and it has a default value in the schema then we will use that - so
	 * we get an array with appropriate content
	 *
	 */
	@SuppressWarnings("unchecked")
	protected void support_arrayDefaultValue(CodegenModel model, CodegenProperty property) {
		if (!property.getIsArrayModel() && !property.getIsListContainer()) {
			// we have nothing to do - not an array
			return;
		}
		// does it have a default set in schema?
		if (!CodegenUtil.hasPropertyUserAssignedDefaultValue(model, property)) {

			// OK this array does not have any default items
			// if the property is nullable it does not make sense to create an ArrayList object so lets null it
			// out
			if (property.nullable) {
				property.defaultValue = "null";

				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"this array does not have default and nullable - so let's keep it on NULL then");
			}

			if (CodegenUtil.hasPropertyDefaultValue(model, property)) {
				// just let the user know that Codegen has assigned an empty value to this
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"non-nullable property so Codegen applied a default empty array to it automatically because it is possible with this type");
			}

			return;
		}

		// OK so this is an array and has default value defined in schema - let's get that!

		ObjectMapper objectMapper = new ObjectMapper();
		HashMap<String, Object> parseResult = null;
		Exception parsingException = null;
		try {
			parseResult = objectMapper.readValue(property.jsonSchema, HashMap.class);
		} catch (Exception e) {
			parsingException = e;
		}
		Preconditions.checkState(parseResult != null,
				"Oops it looks we failed to json parse $ref attribute at property %s.%s! jsonSchema\n%s\nlead to error: %s",
				model.name, property.baseName, property.jsonSchema, parsingException);

		List<Object> defaultValues = (List<Object>) parseResult.get("default");

		// for now we just support primitive types
		if (defaultValues.size() > 0) {
			Class<?> itemClass = defaultValues.get(0).getClass();
			if (!String.class.equals(itemClass) && !Boolean.class.equals(itemClass)
					&& !Number.class.isAssignableFrom(itemClass)) {

				PropertyInlineMessages.appendToProperty(property, ModelMessageType.WARNING,
						"default value was set but for now we just support primitive types in default array - we skipped this one");
				return;
			}

			PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
					"default array value was set in schema - applied here");

			// OK let's create the string representation of the item list
			String jsonStr = null;
			try {
				jsonStr = objectMapper.writeValueAsString(defaultValues);
			} catch (JsonProcessingException e) {
				// oops
				throw new IllegalStateException("We failed to produce array default value of " + model.name + "."
						+ property.baseName + " due to json serialization error: " + e);
			}
			// let's string hack a bit! ;-)
			jsonStr = jsonStr.replace("[", "").replace("]", "");
			property.defaultValue = property.defaultValue.replace("<>()", "<>(Arrays.asList(" + jsonStr + "))");
		}

	}

	@SuppressWarnings("unchecked")
	protected void support_outputOnlyIfNonDefault(CodegenModel model, CodegenProperty property) {
		List<String> serializeOnlyIfNonDefaultProperties = null;
		if (model.getVendorExtensions().containsKey(X_OBJECT_SERIALIZE_ONLY_IF_NON_DEFAULT_PROPERTIES)) {
			// if set on Model level then it takes precedence over globl option
			serializeOnlyIfNonDefaultProperties = (List<String>) model.getVendorExtensions()
					.get(X_OBJECT_SERIALIZE_ONLY_IF_NON_DEFAULT_PROPERTIES);
		}

		boolean addJsonInclude_NonDefault = serializeOnlyIfNonDefaultProperties != null
				&& serializeOnlyIfNonDefaultProperties.contains(property.baseName);

		if (addJsonInclude_NonDefault) {
			if (additionalProperties.containsKey("jackson")) {
				model.imports.add("JsonInclude");
				model.imports.add("JsonInclude.Include");

				LOGGER.info("model {}, field '{}': due to {} setting necessary annotations will be added", model.name,
						property.baseName, X_OBJECT_SERIALIZE_ONLY_IF_NON_DEFAULT_PROPERTIES);
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"listed in '" + X_OBJECT_SERIALIZE_ONLY_IF_NON_DEFAULT_PROPERTIES
								+ "' - so necessary annotations added here OR to the getter");

			} else {
				// TODO does this have a Gson alternative maybe???
			}
		}

		// finally write back the result - so the template rendering can also see that if needed
		property.getVendorExtensions().put(X_COMPUTED_PROPERTY_SERIALIZE_ONLY_IF_NON_DEFAULT_FLAG,
				addJsonInclude_NonDefault);
	}

	protected void support_keepPropertyNames(CodegenModel model, CodegenProperty property) {
		boolean modelKeepPropertyNames = keepPropertyNames;
		boolean keepPropertyName = keepPropertyNames;

		if (model.getVendorExtensions().containsKey(X_OBJECT_KEEP_PROPERTY_NAMES_FLAG)) {
			// if set on Model level then it takes precedence over globl option
			modelKeepPropertyNames = getBooleanValue(model, X_OBJECT_KEEP_PROPERTY_NAMES_FLAG);
		}

		// property level flag takes precedence here - if set
		if (property.getVendorExtensions().containsKey(X_PROPERTY_KEEP_PROPERTY_NAME_FLAG)) {
			keepPropertyName = getBooleanValue(property, X_PROPERTY_KEEP_PROPERTY_NAME_FLAG);
		} else {
			// if not set then inherits from Model level
			keepPropertyName = modelKeepPropertyNames;
		}
		// if we keep the name of this property then let's do it!
		if (!property.baseName.equals(property.name)) {
			if (keepPropertyName) {
				property.setName(property.baseName);
				property.setGetter("get_" + property.baseName);
				property.setSetter("set_" + property.baseName);

				LOGGER.info(
						"model {}, field '{}': due to 'keepPropertyName' settings original name is kept and getter '{}' / setter '{}' will be used",
						model.name, property.baseName, property.getter, property.setter);
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"due to 'keepPropertyName' settings original name is kept");
				PropertyInlineMessages.appendToGetter(property, ModelMessageType.EXPLANATION,
						"due to 'keepPropertyName' settings original name of field is kept and for getter we go with 'get_<fieldName>' pattern");
				PropertyInlineMessages.appendToSetter(property, ModelMessageType.EXPLANATION,
						"due to 'keepPropertyName' settings original name of field is kept and for setter we go with 'set_<fieldName>' pattern");
			} else {
				LOGGER.info("model {}, field '{}': name is changed to '{}' and getter '{}' / setter '{}' will be used",
						model.name, property.baseName, property.name, property.getter, property.setter);
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"name is changed, in schema the original name is: '" + property.baseName + "'");
			}
		}

		// finally write back the result - so the template rendering can also see that if needed
		property.getVendorExtensions().put(X_PROPERTY_KEEP_PROPERTY_NAME_FLAG, keepPropertyName);
		model.getVendorExtensions().put(X_OBJECT_KEEP_PROPERTY_NAMES_FLAG, modelKeepPropertyNames);
	}

	protected String getPrimitiveTypeDefaultValue(String primitiveType) {
		if ("boolean".equals(primitiveType)) {
			return "false";
		}
		if ("byte".equals(primitiveType) || "short".equals(primitiveType) || "int".equals(primitiveType)
				|| "long".equals(primitiveType)) {
			return "0";
		}
		if ("float".equals(primitiveType)) {
			return "0f";
		}
		if ("double".equals(primitiveType)) {
			return "0d";
		}
		throw new IllegalArgumentException(
				primitiveType + " is not a known primitive type for providing default value");
	}

	/**
	 * @param property
	 *            the property in question
	 * @return if the property's datatype has a primitive pair then it is returned - NULL is returned
	 *         otherwise
	 */
	protected String getUsablePrimitiveType(CodegenProperty property) {
		return wrapperToPrimitiveTypeMapping.get(property.baseType);
	}

	/**
	 * Checks the property attributes and tells if there are any reasons not to use primitive datatype
	 *
	 * @param property
	 * @return NULL if property is eligible to use primitive type - the reason "why not?" otherwise
	 */
	protected String isPropertyEligibleForUsingPrimitiveDatatype(CodegenProperty property) {
		if (!property.nullable) {
			return "property is nullable:false - null-check is an undefined operation on a primitive type";
		}
		return null;
	}

	protected void support_usePrimitiveTypesIfPossible(CodegenModel model, CodegenProperty property) {

		// as a first step let's check if property is eligible to use primitive datatype
		// and if not, store the reason
		String canNotUsePrimitiveTypeReason = null;
		String usablePrimitiveType = getUsablePrimitiveType(property);
		if (usablePrimitiveType == null) {
			canNotUsePrimitiveTypeReason = "data type '" + property.baseType + "' has no primitive type alternative";
		} else {
			// OK we have a primitive type
			// but are there any other reasons why not use it?
			canNotUsePrimitiveTypeReason = isPropertyEligibleForUsingPrimitiveDatatype(property);
		}
		boolean canUsePrimitiveType = canNotUsePrimitiveTypeReason == null;

		// OK, now check if we need/want to use primitive type
		boolean usePrimitiveType = getBooleanValue(property, X_PROPERTY_USE_PRIMITIVE_TYPE);
		boolean usePrimitiveTypeIfPossible = getBooleanValue(model, X_OBJECT_USE_PRIMITIVE_TYPES_IF_POSSIBLE)
				|| this.usePrimitiveTypesIfPossible;

		// let's evaluate...

		boolean primitiveTypeUsed = false;
		if (usePrimitiveType) {
			// OK so user said: do it! I need it here!
			if (canUsePrimitiveType) {
				primitiveTypeUsed = true;
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"primitive type is used because it is enforced on property level by '"
								+ X_PROPERTY_USE_PRIMITIVE_TYPE + ": true' flag");
			} else {
				// this is a strong failure!
				throw new SchemaValidationException("unsatisfiable wish - generation must abort! In model '"
						+ model.name + "', field '" + property.baseName
						+ "' it was told to use primitive data type but it is not possible because: "
						+ canNotUsePrimitiveTypeReason);
			}
		}
		if (!primitiveTypeUsed && usePrimitiveTypeIfPossible) {
			// OK so user said: do it if possible, but if not then leave it
			if (canUsePrimitiveType) {
				primitiveTypeUsed = true;

				if (getBooleanValue(model, X_OBJECT_USE_PRIMITIVE_TYPES_IF_POSSIBLE)) {
					PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
							"primitive type is used because a) it can b) '" + X_OBJECT_USE_PRIMITIVE_TYPES_IF_POSSIBLE
									+ ": true' is set on parent object");
				} else {
					PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
							"primitive type is used because a) it can b) option '" + OPT_USE_PRIMITIVE_TYPES_IF_POSSIBLE
									+ "=true' is used in generator setup");
				}

			} else {
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"however we should use primitive type but we can not for this field because: "
								+ canNotUsePrimitiveTypeReason);

			}
		}

		// and let's do it if thats the decision
		if (primitiveTypeUsed) {
			property.baseType = usablePrimitiveType;
			property.datatype = usablePrimitiveType;
			property.datatypeWithEnum = usablePrimitiveType;
			property.defaultValue = getPrimitiveTypeDefaultValue(usablePrimitiveType);
		}

	}

	@Override
	public String getModelFullyQualifiedName(String modelName) {
		String fqName = ignoreImportMapping ? null : importMapping.get(modelName);
		if (fqName == null) {
			fqName = modelPackage + "." + modelName;
		}
		return fqName;
	}

	/**
	 * We override this method because this guy in the Java generation is actually detecting
	 * "conflicting" field names and renaming them automatically. We want to be able to capture these
	 * renames and mark those vars as these auto-renames definitely lead to some side effects. Makes the
	 * generated models fuzzy and ugly - so maybe we want to fail the build in these cases
	 *
	 * note: the renaming mechanism in this method also had problems. As a contribution code fix was
	 * done and ticket https://github.com/swagger-api/swagger-codegen-generators/issues/1066 was raised
	 */
	@Override
	protected void fixUpParentAndInterfaces(CodegenModel codegenModel, Map<String, CodegenModel> allModels) {
		// if ("DogResponseClass".equals(codegenModel.name) ||
		// "ExtendedFieldClass".equals(codegenModel.name)) {
		// LOGGER.info("buu");
		// }

		// in order being able to revert the renames calling super. will cause we need to store a copy of
		// the original vars
		// This way we can detect renames later and revert them

		Map<String, OriginalPropertyNames> copyOfProperties = new HashMap<>();
		for (CodegenProperty codegenProperty : codegenModel.vars) {
			OriginalPropertyNames originalNames = new OriginalPropertyNames(codegenProperty);
			copyOfProperties.put(codegenProperty.baseName, originalNames);
		}

		// now let's invoke super
		super.fixUpParentAndInterfaces(codegenModel, allModels);

		/*
		 * ========= remove BEGIN
		 * remove this once https://github.com/swagger-api/swagger-codegen-generators/issues/1066 is fixed
		 */

		// let's restore renames
		for (CodegenProperty codegenProperty : codegenModel.vars) {
			OriginalPropertyNames originalNames = copyOfProperties.get(codegenProperty.baseName);
			if (!originalNames.name.equals(codegenProperty.name)) {
				originalNames.restoreNames(codegenProperty);
			}
		}
		// and apply fixed method
		fixOf1066_fixUpParentAndInterfaces(codegenModel, allModels);

		/*
		 * ========= remove END
		 */

		// now let's recognize what was renamed and mark them!
		for (CodegenProperty codegenProperty : codegenModel.vars) {
			OriginalPropertyNames originalNames = copyOfProperties.get(codegenProperty.baseName);
			if (!originalNames.name.equals(codegenProperty.name)) {
				codegenProperty.vendorExtensions.put(X_PROPERTY_CONFLICTING_AND_RENAMED, true);
				codegenProperty.vendorExtensions.put(X_PROPERTY_ORIGINAL_NAMES, originalNames);

				// let's also collect info about conflicting with what and where?
				CodegenModel conflictingModel = null;
				CodegenModel parentModel = codegenModel.parentModel;
				while (parentModel != null) {
					if (parentModel.vars == null || parentModel.vars.isEmpty()) {
						parentModel = parentModel.parentModel;
						continue;
					}
					boolean hasConflict = parentModel.vars.stream()
							.anyMatch(parentProperty -> (parentProperty.name.equals(originalNames.name)
									|| parentProperty.getGetter().equals(originalNames.getter)
									|| parentProperty.getSetter().equals(originalNames.setter)
											&& !parentProperty.datatype.equals(codegenProperty.datatype)));
					if (hasConflict) {
						conflictingModel = parentModel;
						break;
					}
					parentModel = parentModel.parentModel;
				}

				// let's add some precaution...
				Preconditions.checkState(conflictingModel != null, "Oops! We have a bug...");

				codegenProperty.vendorExtensions.put(X_PROPERTY_CONFLICTING_MODEL, conflictingModel);

				// add some field comments
				CodegenProperty superProperty = CodegenUtil.getPropertyByBaseName(conflictingModel,
						codegenProperty.baseName);
				PropertyInlineMessages.appendToProperty(codegenProperty, ModelMessageType.EXPLANATION,
						"overriding '" + conflictingModel.name + "." + superProperty.name + "'");
			}
		}

	}

	/**
	 * TEMPORARY METHOD! Until https://github.com/swagger-api/swagger-codegen-generators/issues/1066 is
	 * not fixed, this method mimics the fix. Remove this once the fix is in place!
	 *
	 * @param codegenModel
	 * @param allModels
	 */
	private void fixOf1066_fixUpParentAndInterfaces(CodegenModel codegenModel, Map<String, CodegenModel> allModels) {
		if (codegenModel.vars == null || codegenModel.vars.isEmpty() || codegenModel.parentModel == null) {
			return;
		}

		for (CodegenProperty codegenProperty : codegenModel.vars) {

			CodegenModel parentModel = codegenModel.parentModel;

			while (parentModel != null) {
				if (parentModel.vars == null || parentModel.vars.isEmpty()) {
					parentModel = parentModel.parentModel;
					continue;
				}
				boolean hasConflict = parentModel.vars.stream()
						.anyMatch(parentProperty -> (parentProperty.name.equals(codegenProperty.name)
								|| parentProperty.getGetter().equals(codegenProperty.getGetter())
								|| parentProperty.getSetter().equals(codegenProperty.getSetter())
										&& !parentProperty.datatype.equals(codegenProperty.datatype)));
				if (hasConflict) {
					codegenProperty.name = toVarName(codegenModel.name + "_" + codegenProperty.name);
					codegenProperty.nameInCamelCase = camelize(codegenProperty.name, false);
					codegenProperty.getter = toGetter(codegenProperty.name);
					codegenProperty.setter = toSetter(codegenProperty.name);
					break;
				}
				parentModel = parentModel.parentModel;
			}

		}
	}

	/**
	 * This method can just run after {@link #fixUpParentAndInterfaces(CodegenModel, Map)} is finished
	 * in all models! The method scans through all model all properties and where it detects
	 * {@link IKeytilesCodegen#X_PROPERTY_CONFLICTING_AND_RENAMED} then it will check compatibility btw
	 * the model's property and the overriden super property and adding the result of this compatibility
	 * check into {@link IKeytilesCodegen#X_PROPERTY_SUPER_IS_ASSIGNABLE} flag
	 *
	 * @param allModels
	 */
	private void enrichModelPropertiesWithSuperIsAssignableFlag(Map<String, CodegenModel> allModels) {

		for (CodegenModel codegenModel : allModels.values()) {

			CodegenUtil.validateModelState(codegenModel, ModelState.baseCodegenFullyEnriched);

			if ("DogResponseClass".equals(codegenModel.name) || "ExtendedFieldClass".equals(codegenModel.name)) {
				LOGGER.info("buu");
			}

			// now let's recognize what was renamed and mark them!
			for (CodegenProperty property : codegenModel.vars) {
				if (property.getBooleanValue(X_PROPERTY_CONFLICTING_AND_RENAMED)) {
					CodegenModel conflictingModel = (CodegenModel) property.vendorExtensions
							.get(X_PROPERTY_CONFLICTING_MODEL);

					CodegenUtil.validateModelState(conflictingModel, ModelState.baseCodegenFullyEnriched);

					CodegenProperty superProperty = CodegenUtil.getPropertyByBaseName(conflictingModel,
							property.baseName);
					boolean isSuperAssignable = CodegenUtil.isPropertyAssignableFromProperty(superProperty, property,
							allModels.values());

					property.vendorExtensions.put(IKeytilesCodegen.X_PROPERTY_SUPER_IS_ASSIGNABLE, isSuperAssignable);

				}
			}

			codegenModel.vendorExtensions.put(X_MODEL_STATE, ModelState.fullyEnriched);

		}
	}

	/**
	 * This method works on {@link ModelState#fullyEnriched} models. Iterating over all of the models
	 * and making final decisions about field names.
	 * <p>
	 * {@link #fixUpParentAndInterfaces(CodegenModel, Map)} might rename conflicting fields (marked with
	 * {@link IKeytilesCodegen#X_PROPERTY_CONFLICTING_AND_RENAMED} flag) but in many cases we can rename
	 * those fields back.
	 *
	 */
	private void finalizeModelFieldNamesAndFieldMethods(CodegenModel codegenModel, ModelExtraInfo extraInfo) {

		String methodNameForLog = "finalizeModelFieldNamesAndFieldMethods";

		CodegenUtil.validateModelState(codegenModel, ModelState.fullyEnriched);

		for (CodegenProperty property : codegenModel.vars) {
			if (property.getBooleanValue(X_PROPERTY_CONFLICTING_AND_RENAMED)) {

				OriginalPropertyNames originalNames = (OriginalPropertyNames) property.vendorExtensions
						.get(X_PROPERTY_ORIGINAL_NAMES);

				CodegenModel conflictingModel = (CodegenModel) property.vendorExtensions
						.get(X_PROPERTY_CONFLICTING_MODEL);
				CodegenProperty superProperty = CodegenUtil.getPropertyByBaseName(conflictingModel, property.baseName);
				boolean isSuperAssignable = property.getBooleanValue(IKeytilesCodegen.X_PROPERTY_SUPER_IS_ASSIGNABLE);

				ModelExtraInfo conflictingModelExtraInfo = ModelExtraInfo.getExtraInfo(conflictingModel, this);

				// the first thing we need to check is: methods related to deal with the field!
				// its important because they can cause headache if overloading would not work
				String propertyVisibility = extraInfo.getVisibilityOfPropertyWithBaseName(property.baseName);
				String superPropertyVisibility = conflictingModelExtraInfo
						.getVisibilityOfPropertyWithBaseName(property.baseName);

				boolean hasGetterMethod = !"public".equals(propertyVisibility);
				boolean superHasGetterMethod = !"public".equals(superPropertyVisibility);
				boolean hasSetterMethod = !"public".equals(propertyVisibility)
						&& !"private final".equals(propertyVisibility);
				boolean superHasSetterMethod = !"public".equals(superPropertyVisibility)
						&& !"private final".equals(superPropertyVisibility);
				// generic types like List<?> or Map<?,?> are definitely causing issues with method overriding
				// so we must know if the datatype is generic...
				boolean isGenericDatatype = property.getIsListContainer() || property.getIsMapContainer()
						|| property.getIsArrayModel();
				boolean superIsGenericDatatype = superProperty.getIsListContainer() || superProperty.getIsMapContainer()
						|| property.getIsArrayModel();

				// as now we collected some info about the properties, let's phrase some rules!
				// we definitely prefer to rename back the field in all possible cases!
				boolean renameFieldBack = true;
				List<String> reasons = new LinkedList<>();

				if (hasGetterMethod && superHasGetterMethod) {
					if (!isSuperAssignable) {
						reasons.add("getter methods would collide - datatypes are not compatible");
						renameFieldBack = false;
					} else if (isGenericDatatype) {
						// so we have List<> or Map<>
						// and OK the values are compatible, but there would be problems caused by the generic...
						reasons.add(
								"getter methods would collide - although types are compatible due to generic type compiler would indicate incompatibility");
						renameFieldBack = false;
					}
				}

				if (hasSetterMethod && superHasSetterMethod) {
					if (isGenericDatatype) {
						reasons.add(
								"setter methods would collide - due to generic type compiler would indicate 'name clash' error");
					}
				}

				// finally, at least for know let's do not allow changing visibilities!
				if (!superPropertyVisibility.equals(propertyVisibility)) {
					renameFieldBack = false;
					reasons.add("in " + conflictingModel.name + " the field is '" + superPropertyVisibility
							+ "' while in " + codegenModel.name + " it is '" + propertyVisibility
							+ "' which would result in strange model");
				}

				// rename it back if decided
				if (renameFieldBack) {
					// let's restore names
					originalNames.restoreNames(property);

				} else {
					LOGGER.warn("{}: can not rename back {}.{} to {} because: {}", methodNameForLog, codegenModel.name,
							property.name, originalNames.name, reasons);

					// is this allowed?
					if (!allowRenameConflictingFields) {
						throw new SchemaValidationException("Model '" + codegenModel.name + " extends "
								+ conflictingModel.name + "' and property '" + property.baseName
								+ "' overlaps between them.\nWith the current property setup this would cause the following problems: "
								+ reasons
								+ "\nYou might be able to avoid this happening - check section 'Java limitations with generating models' in README!");
					}

					// also add comment to the field
					for (String reason : reasons) {
						PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
								"renamed to '" + property.name + "' because otherwise " + reason);
					}
				}
			}
		}

	}

	/**
	 * On this hook the models are already complete - even {@link CodegenModel#parentModel} is available
	 * BUT Do not change {@link CodegenModel#imports} here - as that is already too late... If you need
	 * that you need to hack through {@link #postProcessAllModels(Map)} hook!
	 * <p>
	 * note FYI: this is invoked from early stage of {@link #postProcessAllModels(Map)} logic
	 *
	 * {@inheritDoc}
	 */
	@Override
	protected void postProcessAllCodegenModels(Map<String, CodegenModel> allModels) {
		allModels.values().forEach(model -> {
			model.vendorExtensions.put(X_MODEL_STATE, ModelState.baseCodegenFullyEnriched);
		});

		super.postProcessAllCodegenModels(allModels);

		enrichModelPropertiesWithSuperIsAssignableFlag(allModels);

		// let's save it! and let's save it as a pointer and not just a copy!
		this.allModels = allModels;

		allModels.values().forEach(model -> {
			CodegenBugfixAndEnhanceHelper.validateOnlySupportedVendorAttributesAreUsedOnModel(this, model);
			CodegenBugfixAndEnhanceHelper.markOwnModel(model, this);
		});

		CodegenBugfixAndEnhanceHelper.fixReferredModelAttributesInheritance(allModels);
		CodegenBugfixAndEnhanceHelper.validateModelsAgainstKnownContradictions(allModels);
		CodegenBugfixAndEnhanceHelper.ensureNoConflictBetweenNameOfOwnModelsAndImportedModels(allModels, this);

	}

	/**
	 * During this work we noticed issues even with the orig {@link JavaClientCodegen}... This method
	 * validates the model and breaks the build in case it is recognizing a situation the generation
	 * would break or would be wrong. Then it will raise an exception.
	 *
	 * @param theModel
	 *            the model candidate
	 */
	protected void canModelBeGenerated(CodegenModel theModel) {

		if (theModel.parentModel != null && theModel.parentModel.getIsEnum()) {
			// it is not possible to extend an enum

			throw new SchemaValidationException("We can not generate this schema :-( We ran into a class '"
					+ theModel.name + "' which is extending an enum '" + theModel.parent
					+ "' and this is buggy in Java codegen. See: https://github.com/swagger-api/swagger-codegen/issues/11821\nProbably this is caused by an 'allOf' composition referring in an Enum. You need to remove it and do your schema differently!");
		}

	}

	/**
	 * This is recognizing anyOf, oneOf, allOf (well this last one not for now - see
	 * .canModelBeGenerated()) compositions of Enums and merging/replacing them with one Enum
	 *
	 * @param objs
	 *            the input of the {@link AbstractJavaCodegen#postProcessAllModels(Map)} method
	 * @return modified map - it is possible some models (fabricated) are removed from the generation
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> support_enumCompositions(Map<String, Object> objs) {

		// let's iterate over all entries and check / hunt for enum composition models!

		// we will collect up all stuff we replaced during this turn
		Map<String, CodegenModel> replacedEnums = new HashMap<>();

		objs.entrySet().forEach(modelEntry -> {
			CodegenModel theModel = CodegenUtil.extractModelClassFromPostProcessAllModelsInput(modelEntry);

			if ("ExtendedErrorCodesAnyOf".equals(theModel.name)) {
				LOGGER.info("buu");
			}

			CodegenModel joinedEnumModel = null;
			try {
				// this can return null - if not appropriate for merging
				joinedEnumModel = CodegenUtil.getComposedEnumModelAsMergedEnumModel(theModel, addExplanationsToModel);
			} catch (Exception e) {
				throw new IllegalStateException(
						"Oops! Failed to merge Enum composition in model '" + theModel + "': " + e.getMessage(), e);
			}

			// let's replace the type
			if (joinedEnumModel != null) {
				CodegenUtil.replaceModelDefinitionInPostProcessAllModelsInput(objs, modelEntry.getKey(),
						joinedEnumModel);
				replacedEnums.put(modelEntry.getKey(), joinedEnumModel);
			}
		});

		// next step: let's discover enums who are equal to each other so potentially they can be replaced
		// with each other
		replacedEnums.entrySet().forEach(modelEntry -> {
			Set<String> equalsTo = new HashSet<>();
			replacedEnums.entrySet().forEach(modelEntry2 -> {
				if (modelEntry.getValue() != modelEntry2.getValue()
						&& CodegenUtil.areEnumModelsEqual(modelEntry.getValue(), modelEntry2.getValue())) {
					equalsTo.add(modelEntry2.getKey());
				}
			});
			modelEntry.getValue().getVendorExtensions().put(IKeytilesCodegen.X_MODEL_ENUM_EQUALS_TO, equalsTo);

			// boolean schemaDefined = modelEntry.getValue()
			// .getBooleanValue(IKeytilesCodegen.X_MODEL_SCHEMA_DEFINED_MERGED_ENUM);
			// LOGGER.info("=== enum {} (schema-defined: {}): equals to: {}", modelEntry.getKey(),
			// schemaDefined,
			// equalsTo);
		});

		// and finally lets merge enums!
		// if there are schema-defined enums which are equal to fabricated enums let's remove the fabricated
		// enums and repoint usage points to the schema-defined ones
		Map<String, Object> allProcessedModelsResult = new HashMap<>(objs);
		replacedEnums.entrySet().forEach(modelEntry -> {
			if (modelEntry.getValue().getBooleanValue(IKeytilesCodegen.X_MODEL_SCHEMA_DEFINED_MERGED_ENUM)) {
				Set<String> equalsToEnums = (Set<String>) modelEntry.getValue().getVendorExtensions()
						.get(IKeytilesCodegen.X_MODEL_ENUM_EQUALS_TO);
				if (equalsToEnums != null) {
					// let's iterate over everyone the enum model is equals to
					for (String equalsToEnumName : equalsToEnums) {
						CodegenModel eualsToEnumModel = CodegenUtil.extractModelClassFromPostProcessAllModelsInput(objs,
								equalsToEnumName);
						// if this one is not directly declared then let's replace it with the directly declared (and
						// equals) one!
						if (!eualsToEnumModel.getBooleanValue(IKeytilesCodegen.X_MODEL_SCHEMA_DEFINED_MERGED_ENUM)) {
							LOGGER.info(
									"=== replace - fabricated (by Codegen) enum '{}' will be removed and replaced with '{}' as they are equal",
									eualsToEnumModel.name, modelEntry.getValue().name);

							CodegenUtil.replaceModelReferenceInPostProcessAllModelsInput(objs, equalsToEnumName,
									modelEntry.getKey(), "enum '" + modelEntry.getKey()
											+ "' is defined in the schema directly and it equals to the one you defined inline - so field is reusing that one instead of a fabricated type");
							allProcessedModelsResult.remove(equalsToEnumName);
						}
					}
				}

			}
		});

		return allProcessedModelsResult;
	}

	/**
	 * This is the last hook - just before {@link DefaultGenerator} (see generateModels() private
	 * method!) really starts to iterate over and load/render mustache templates.
	 * <p>
	 * About the return value: if you want to filter out a model(s) from being generated this is the
	 * place you can do! Just simply remove them from the returned map! see
	 * {@link #excludeModelsFromGeneration}!
	 * <p>
	 * In the input you get the template variables and all objects in it here. You can grab the
	 * {@link CodegenModel} using private method
	 * {@link #extractModelClassFromPostProcessAllModelsInput(java.util.Map.Entry)} but keep in mind -
	 * at this point lots of things are already in template vars, e.g. if you change
	 * {@link CodegenModel#imports} that will have no effect
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> postProcessAllModels(Map<String, Object> objs) {
		Map<String, Object> allProcessedModels = super.postProcessAllModels(objs);

		allProcessedModels = support_enumCompositions(allProcessedModels);

		// we will scan stuff and add necessary new render template variables
		allProcessedModels.entrySet().forEach(modelEntry -> {
			Map<String, Object> modelMap = (Map<String, Object>) modelEntry.getValue();
			CodegenModel theModel = CodegenUtil.extractModelClassFromPostProcessAllModelsInput(modelEntry);

			if ("FruitEnum".equals(theModel.name) || "StatApiEndpointProblemClass".equals(theModel.name)
					|| "DogResponseClass".equals(theModel.name)) {
				LOGGER.info("buu");
			}

			canModelBeGenerated(theModel);

			if (modelStyle == ModelStyle.simpleConsistent) {
				ModelExtraInfo extraInfo = ModelExtraInfo.getExtraInfo(theModel, this);

				// finishFixUpParentAndInterfaces(theModel, extraInfo);
				finalizeModelFieldNamesAndFieldMethods(theModel, extraInfo);

				modelMap.put(TPLVAR_CTOR_NEEDS_CONSTRUCTOR, extraInfo.needsConstructor());
				modelMap.put(TPLVAR_CTOR_FOR_SUPER_ARGS, extraInfo.getCtorForSuperArguments());
				modelMap.put(TPLVAR_CTOR_PASS_TO_SUPER_ARGS, extraInfo.getCtorPassToSuperArguments());
				modelMap.put(TPLVAR_CTOR_OWN_FIELD_ARGS, extraInfo.getCtorOwnFieldArguments());
				modelMap.put(TPLVAR_CTOR_VALIDATE_NONNULL_VALUE_ARGS, extraInfo.getCtorValidateNonNullValueArguments());

				String ctorArgAnnonation = null;
				if (additionalProperties.containsKey("jackson")) {
					ctorArgAnnonation = "@JsonProperty(\"{argName}\")";
				} else if (additionalProperties.containsKey("gson")) {
					// TODO is this working? its just a guess... test this Gson?
					ctorArgAnnonation = "@SerializedName(\"{argName}\")";
				}
				modelMap.put(TPLVAR_CTOR_COMBINED_ARGS,
						extraInfo.getConstructorCombinedArgsAsString(ctorArgAnnonation));

				modelMap.put(TPLVAR_PRIVATE_FINAL_FIELDS, extraInfo.getPrivateFinalFields());
				modelMap.put(TPLVAR_PRIVATE_FIELDS, extraInfo.getPrivateFields());
				modelMap.put(TPLVAR_PUBLIC_FIELDS, extraInfo.getPublicFields());

				// do we need imports because of super() things?
				for (CodegenProperty property : extraInfo.getCtorPassToSuperArguments()) {
					/*
					* shit! it's too late here to do this... imports for template resolving are already generated
					* we need a workaround...
					*
					theModel.imports.add(property.baseType);
					*/

					// let's respect type mapping!
					// this can also act as a filter - will return NULL for types we do not need to import (e.g.
					// java.lang.Integer)
					String mappedType = typeMapping.get(property.baseType);
					if (mappedType != null) {
						String fullyQualifiedTypeImport = importMapping.get(mappedType);
						CodegenUtil.addImportToModelMapOnPostProcessAllModelsHook(modelEntry, fullyQualifiedTypeImport);

						LOGGER.info("model {}: injecting import {} - ", theModel.name, fullyQualifiedTypeImport);
					}

					// we also need to ensure that all types of all ctor arguments are imported if needed
					// so now let's focus on the datatype a bit...
					mappedType = typeMapping.get(property.datatype);
					if (mappedType == null) {
						mappedType = property.datatype;
					}
					if (mappedType != null) {
						String fullyQualifiedTypeImport = importMapping.get(mappedType);
						CodegenUtil.addImportToModelMapOnPostProcessAllModelsHook(modelEntry, fullyQualifiedTypeImport);
					}

				}

				// do we have non-zero argument constructor?
				if (extraInfo.needsConstructor()) {
					CodegenUtil.addImportToModelMapOnPostProcessAllModelsHook(modelEntry,
							"com.fasterxml.jackson.annotation.JsonCreator");
				}
			}

		});

		// as a last step let's drop all stuff from the result which we should exclude
		Map<String, Object> allProcessedModelsResult = new HashMap<>(allProcessedModels);
		if (excludeModelsFromGeneration != null) {
			excludeModelsFromGeneration.forEach(modelName -> {
				allProcessedModelsResult.remove(modelName);
			});
		}

		// let's register the models into Maven execution - if we have one
		if (mavenExecutionId != null) {
			MavenExecutionInfo executionInfo = MavenExecutionInfo.getExecutionInfo(mavenExecutionId);
			allProcessedModelsResult.entrySet().forEach(modelEntry -> {
				CodegenModel theModel = CodegenUtil.extractModelClassFromPostProcessAllModelsInput(modelEntry);
				executionInfo.registerModel(theModel.name, theModel);
			});
		}

		return allProcessedModelsResult;
	}

	@Override
	public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
		objs = super.postProcessModelsEnum(objs);
		return objs;
	}

	@Override
	public SchemaParamCollection getAddSchemaModelsToImportMappings() {
		return addSchemaModelsToImportMappings;
	}

	@Override
	public boolean excludeImportMappingsFromGeneration() {
		return excludeImportMappingsFromGeneration;
	}

	@Override
	public void setExcludeModelsFromGeneration(Collection<String> modelNamesToSkip) {
		excludeModelsFromGeneration = new HashSet<>(modelNamesToSkip);
	}

	@Override
	public String getMavenExecutionId() {
		return mavenExecutionId;
	}

	@Override
	public List<String> getAddSchemaModelsToImportMappingsFromMavenExecutions() {
		return addSchemaModelsToImportMappingsFromMavenExecutions == null ? null
				: new ArrayList<>(addSchemaModelsToImportMappingsFromMavenExecutions);
	}

	@Override
	public Set<String> getAllSupportedObjectLevelVendorFieldNames() {
		return new HashSet<>(Arrays.asList( //
				X_OBJECT_KEEP_PROPERTY_NAMES_FLAG, //
				X_OBJECT_SERIALIZE_ONLY_IF_NON_DEFAULT_PROPERTIES, //
				X_OBJECT_USE_PRIMITIVE_TYPES_IF_POSSIBLE //
		));
	}

	@Override
	public Set<String> getAllSupportedPropertyLevelVendorFieldNames() {
		return new HashSet<>(Arrays.asList( //
				X_PROPERTY_KEEP_PROPERTY_NAME_FLAG, //
				X_PROPERTY_USE_PRIMITIVE_TYPE //
		));
	}

	@Override
	public OpenAPI getOpenApi() {
		return openAPI;
	}

	@Override
	public Map<String, CodegenModel> getAllModels() {
		Map<String, CodegenModel> allModels = this.allModels == null ? null
				: Collections.unmodifiableMap(this.allModels);
		return allModels;
	}

}
