package com.keytiles.swagger.codegen;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.keytiles.swagger.codegen.helper.CodegenUtil;
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
	public final static String OPT_ADD_EXPLANATIONS_TO_MODEL = "addExplanationsToModel";
	public final static String OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING = "addSchemaModelsToImportMappings";
	public final static String OPT_EXCLUDE_IMPORT_MAPPINGS_FROM_GENERATION = "excludeImportMappingsFromGeneration";

	public final static String OPT_MAVEN_EXECUTION_ID = "mavenExecutionId";
	public final static String OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS = "addSchemaModelsToImportMappingsFromMavenExecutions";

	public final static String VENDOR_PREFIX = "x-keytiles-";
	public final static String COMPUTED_VENDOR_PREFIX = "x-keytilescomputed-";

	public final static String X_OBJECT_SERIALIZE_ONLY_IF_NON_DEFAULT_PROPERTIES = VENDOR_PREFIX
			+ "serialize-only-if-non-default-properties";
	public final static String X_OBJECT_KEEP_PROPERTY_NAMES_FLAG = VENDOR_PREFIX + "keep-property-names";
	public final static String X_PROPERTY_KEEP_PROPERTY_NAME_FLAG = VENDOR_PREFIX + "keep-property-name";
	public final static String X_OBJECT_USE_PRIMITIVE_TYPES_IF_POSSIBLE = VENDOR_PREFIX
			+ "use-primitive-datatypes-if-possible";
	public final static String X_PROPERTY_USE_PRIMITIVE_TYPE = VENDOR_PREFIX + "use-primitive-datatype";

	public final static String X_COMPUTED_PROPERTY_SERIALIZE_ONLY_IF_NON_DEFAULT_FLAG = COMPUTED_VENDOR_PREFIX
			+ "serialize-only-if-non-default";

	/**
	 * Boolean property added if we merged this enum from composition<br>
	 * see
	 * {@link CodegenUtil#getComposedEnumModelAsMergedEnumModel(io.swagger.codegen.v3.CodegenModel, boolean)}
	 */
	public final static String X_MERGED_ENUM = VENDOR_PREFIX + "merged-enum";
	/**
	 * Boolean property added if we merged this enum from composition and this is a model which was not
	 * directly defined in the schema by user but Codegen fabricated it<br>
	 * see
	 * {@link CodegenUtil#getComposedEnumModelAsMergedEnumModel(io.swagger.codegen.v3.CodegenModel, boolean)}
	 */
	public final static String X_FABRICATED_MERGED_ENUM = VENDOR_PREFIX + "fabricated-merged-enum";
	/**
	 * Boolean property added if we merged this enum from composition and this is a model which was
	 * directly defined in the schema by user<br>
	 * see
	 * {@link CodegenUtil#getComposedEnumModelAsMergedEnumModel(io.swagger.codegen.v3.CodegenModel, boolean)}
	 */
	public final static String X_SCHEMA_DEFINED_MERGED_ENUM = VENDOR_PREFIX + "schema-defined-merged-enum";

	/**
	 * Name of other Enum models with whom this Enum model is basically equals<br>
	 * see
	 * {@link CodegenUtil#areEnumModelsEqual(io.swagger.codegen.v3.CodegenModel, io.swagger.codegen.v3.CodegenModel)}
	 */
	public final static String X_ENUM_EQUALS_TO = VENDOR_PREFIX + "enum-equals-to";

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

	/**
	 * The Codegen implementation should return all "x-" prefixed vendor stuff which it supports on
	 * Object level. This is used in a validation step which ensures that user can not use any other
	 * (maybe became unsupported) of these things accidentally as a left-over from previous versions
	 *
	 * @return all "x-" attribute names supported on Object level
	 */
	public Set<String> getAllSupportedObjectLevelVendorFieldNames();

	/**
	 * The Codegen implementation should return all "x-" prefixed vendor stuff which it supports on
	 * Property level. This is used in a validation step which ensures that user can not use any other
	 * (maybe became unsupported) of these things accidentally as a left-over from previous versions
	 *
	 * @return all "x-" attribute names supported on Object level
	 */
	public Set<String> getAllSupportedPropertyLevelVendorFieldNames();

}
