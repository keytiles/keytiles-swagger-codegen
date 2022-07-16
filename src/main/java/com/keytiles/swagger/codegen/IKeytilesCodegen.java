package com.keytiles.swagger.codegen;

import java.util.Collection;
import java.util.List;

import com.keytiles.swagger.codegen.helper.config.SchemaParamCollection;

import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.CodegenConstants;

public interface IKeytilesCodegen extends CodegenConfig {

	public final static String DEVELOPER_NAME = "Keytiles";
	public final static String DEVELOPER_EMAIL = "support@keytiles.com";
	public final static String DEVELOPER_ORGANIZATION = "Keytiles";
	public final static String DEVELOPER_ORGANIZATION_URL = "https://keytiles.com";

	public final static String OPT_MODEL_STYLE = "modelStyle";
	public final static String OPT_KEEP_PROPERTY_NAMES = "keepPropertyNames";
	public final static String OPT_USE_PRIMITIVE_TYPES_IF_POSSIBLE = "usePrimitiveTypesIfPossible";
	public final static String OPT_NULLABLE_TAG_DEFAULT_VALUE = "nullableTagDefaultValue";
	public final static String OPT_ADD_EXPLANATIONS_TO_MODEL = "addExplanationsToModel";
	public final static String OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING = "addSchemaModelsToImportMappings";
	public final static String OPT_EXCLUDE_IMPORT_MAPPINGS_FROM_GENERATION = "excludeImportMappingsFromGeneration";

	public final static String OPT_MAVEN_EXECUTION_ID = "mavenExecutionId";
	public final static String OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS = "addSchemaModelsToImportMappingsFromMavenExecutions";

	public final static String X_SERIALIZE_ONLY_IF_NON_DEFAULT_FLAG = "x-keytiles-serialize-only-if-non-default";
	public final static String X_KEEP_PROPERTY_NAMES_FLAG = "x-keytiles-keep-property-names";
	public final static String X_KEEP_PROPERTY_NAME_FLAG = "x-keytiles-keep-property-name";
	public final static String X_USE_PRIMITIVE_TYPES_IF_POSSIBLE = "x-keytiles-use-primitive-datatypes-if-possible";
	public final static String X_USE_PRIMITIVE_TYPE = "x-keytiles-use-primitive-datatype";

	/**
	 * Considers the provided {@link #importMapping()} plus {@link #getIgnoreImportMapping()} and
	 * returns a fully qualified Java class name for the given model
	 *
	 * @param modelName
	 *            which model?
	 * @return the fully qualified Java class name
	 */
	public String getModelFullyQualifiedName(String modelName);

	/**
	 * @return should return the collection if addSchemaModelsToImportMappings is set - NULL otherwise
	 */
	public SchemaParamCollection getAddSchemaModelsToImportMappings();

	/**
	 * @return should return TRUE if excludeImportMappingsFromGeneration is set - FALSE otherwise
	 */
	public boolean excludeImportMappingsFromGeneration();

	/**
	 * This is a bit opposite logic then the out of the box provided
	 * {@link CodegenConstants#GENERATE_MODELS} option. Which is an inclusive list. This guy is an
	 * excluding list...
	 *
	 * Exclusion can be made on {@link CodegenConfig#postProcessAllModels(java.util.Map)} hook by simply
	 * leaving out the model
	 *
	 * @param modelNamesToSkip
	 *            the name of the models we should skip out from generation
	 */
	public void setExcludeModelsFromGeneration(Collection<String> modelNamesToSkip);

	public String getMavenExecutionId();

	public List<String> getAddSchemaModelsToImportMappingsFromMavenExecutions();

}
