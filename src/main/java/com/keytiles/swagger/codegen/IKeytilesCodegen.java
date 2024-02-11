package com.keytiles.swagger.codegen;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.keytiles.swagger.codegen.KeytilesJavaCodegen.OriginalPropertyNames;
import com.keytiles.swagger.codegen.helper.CodegenUtil;
import com.keytiles.swagger.codegen.helper.config.SchemaParamCollection;

import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.v3.oas.models.OpenAPI;

public interface IKeytilesCodegen extends CodegenConfig {

	/**
	 * Codegen works the way Models are going through different enrichment steps and this or that fields
	 * / information simply does not exist before certain states. Building logic around them would cause
	 * wrong decisions (checking for example {@link CodegenModel#parentModel} == null) hidden way. So we
	 * introduced this enum and we add this to different states using
	 * {@link IKeytilesCodegen#X_MODEL_STATE}
	 *
	 * @author attilaw
	 *
	 */
	public static enum ModelState {
		// the model is created with .fromModel() but for exampple .parentModel references are not there yet
		// (fixUpParentAndInterfaces() method was not running yet, from .postProcessAllCodegenModels())
		created(0),
		// the original JavaCodegen finished all enrichments - even .parentModel references are there
		baseCodegenFullyEnriched(1),
		// when also KeytilesJavaCodegen enriched the model with every flags and info
		fullyEnriched(2);

		private final int level;

		private ModelState(int level) {
			this.level = level;
		}

		public int getLevel() {
			return this.level;
		}
	}

	public final static String DEVELOPER_NAME = "Keytiles";
	public final static String DEVELOPER_EMAIL = "support@keytiles.com";
	public final static String DEVELOPER_ORGANIZATION = "Keytiles";
	public final static String DEVELOPER_ORGANIZATION_URL = "https://keytiles.com";

	public final static String OPT_MODEL_STYLE = "modelStyle";
	public final static String OPT_KEEP_PROPERTY_NAMES = "keepPropertyNames";
	public final static String OPT_ALLOW_RENAME_CONFLICTING_FIELDS = "allowRenameConflictingFields";
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
	 * Boolean property added to a model if that model is defined in the schema (see
	 * {@link DefaultCodegenConfig#getInputURL()}) directly which we are generating now - and not just
	 * imported from another "remote" schema file
	 */
	public final static String X_MODEL_IS_OWN_MODEL = VENDOR_PREFIX + "is-own-model";

	/**
	 * Boolean property added if we merged this enum from composition<br>
	 * see
	 * {@link CodegenUtil#getComposedEnumModelAsMergedEnumModel(io.swagger.codegen.v3.CodegenModel, boolean)}
	 */
	public final static String X_MODEL_MERGED_ENUM = VENDOR_PREFIX + "merged-enum";
	/**
	 * Boolean property added if we merged this enum from composition and this is a model which was not
	 * directly defined in the schema by user but Codegen fabricated it<br>
	 * see
	 * {@link CodegenUtil#getComposedEnumModelAsMergedEnumModel(io.swagger.codegen.v3.CodegenModel, boolean)}
	 */
	public final static String X_MODEL_FABRICATED_MERGED_ENUM = VENDOR_PREFIX + "fabricated-merged-enum";
	/**
	 * Boolean property added if we merged this enum from composition and this is a model which was
	 * directly defined in the schema by user<br>
	 * see
	 * {@link CodegenUtil#getComposedEnumModelAsMergedEnumModel(io.swagger.codegen.v3.CodegenModel, boolean)}
	 */
	public final static String X_MODEL_SCHEMA_DEFINED_MERGED_ENUM = VENDOR_PREFIX + "schema-defined-merged-enum";

	/**
	 * Name of other Enum models with whom this Enum model is basically equals<br>
	 * see
	 * {@link CodegenUtil#areEnumModelsEqual(io.swagger.codegen.v3.CodegenModel, io.swagger.codegen.v3.CodegenModel)}
	 */
	public final static String X_MODEL_ENUM_EQUALS_TO = VENDOR_PREFIX + "enum-equals-to";

	/**
	 * This is a marker flag we put to Properties on objects who are extending other objects and
	 * overriding a field with same name but different type - generating a conflict for it. The
	 * AbstractJavaCodegen.fixUpParentAndInterfaces() method detects and does the rename in these cases
	 */
	public final static String X_PROPERTY_CONFLICTING_AND_RENAMED = VENDOR_PREFIX + "conflicting-and-renamed";

	/**
	 * If {@link #X_PROPERTY_CONFLICTING_AND_RENAMED} is set then this is a
	 * {@link OriginalPropertyNames} object
	 */
	public final static String X_PROPERTY_ORIGINAL_NAMES = VENDOR_PREFIX + "original-names";

	/**
	 * If {@link #X_PROPERTY_CONFLICTING_AND_RENAMED} is set then this one is the model in which we have
	 * the conflict detected (it is for sure our superclass)
	 */
	public final static String X_PROPERTY_CONFLICTING_MODEL = VENDOR_PREFIX + "conflicting-model";

	/**
	 * Boolean property. If {@link #X_PROPERTY_CONFLICTING_AND_RENAMED} is set then it is checked if the
	 * variable in the superclass is able to take this value or not - see
	 * {@link CodegenUtil#isPropertyAssignableFromProperty(io.swagger.codegen.v3.CodegenProperty, io.swagger.codegen.v3.CodegenProperty, Collection)}
	 */
	public final static String X_PROPERTY_SUPER_IS_ASSIGNABLE = VENDOR_PREFIX + "super-is-assignable";

	/**
	 * Added to Models - see {@link ModelState}!
	 */
	public final static String X_MODEL_STATE = VENDOR_PREFIX + "model-state";

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

	/**
	 * @return returns the OpenAPI spec (which is normally just hidden)
	 */
	public OpenAPI getOpenApi();

	/**
	 * Gives back all the models we have in this codegen context BUT!<br>
	 * IMPORTANT! This is available only after the {@link DefaultCodegenConfig}.postProcessAllModels()
	 * hook!
	 *
	 * @return A readonly snapshot of all models in the context or NULL if you invoked this too early!
	 */
	public Map<String, CodegenModel> getAllModels();

}
