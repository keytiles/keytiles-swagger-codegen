package com.keytiles.swagger.codegen.helper.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.KeytilesJavaCodegen;
import com.keytiles.swagger.codegen.helper.maven.MavenExecutionInfo;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public class ConfigOptionHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(KeytilesJavaCodegen.class);

	private ConfigOptionHelper() {
	}

	/**
	 * This helps to implement the {@link IKeytilesCodegen#OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING}
	 * option.
	 * <p>
	 * It scans through the given schema collection and returns all the entries you should simply just
	 * add to the import mappings
	 *
	 * @param schemaCollection
	 */
	private static Map<String, String> getImportMappingsToAdd(SchemaParamCollection schemaCollection) {
		final Map<String, String> importMappings = new HashMap<>();

		// note: as you can see we will process in order (of additions) here!
		schemaCollection.getSchemaParamsInAdditionOrder().entrySet().forEach(schemaParamEntry -> {
			// take the model names scanned from this schema
			Map<String, Schema> schemas = schemaParamEntry.getValue().getOpenAPI().getComponents().getSchemas();
			schemas.entrySet().forEach(schemaEntry -> {
				// check the already existing imports and if not there yet (this is why order is important!) then
				// add
				if (!importMappings.containsKey(schemaEntry.getKey())) {
					importMappings.put(schemaEntry.getKey(),
							schemaParamEntry.getValue().getModelPackage() + "." + schemaEntry.getKey());
				}
			});
		});

		return importMappings;
	}

	private static Map<String, String> getImportMappingsFromMavenExecutions(List<String> executionIds) {
		final Map<String, String> importMappings = new HashMap<>();

		for (String executionId : executionIds) {
			MavenExecutionInfo executionInfo = MavenExecutionInfo.getExecutionInfo(executionId);
			Preconditions.checkState(executionInfo != null,
					"Oops! It looks Maven execution with id '%s' is not found! Was this running? pom.xml error?",
					executionId);

			boolean addOwnImportMappingsToo = true;
			executionInfo.getModelsForImportMapping(addOwnImportMappingsToo).entrySet().forEach(schemaEntry -> {
				// check the already existing imports and if not there yet (this is why order is important!) then
				// add
				if (!importMappings.containsKey(schemaEntry.getKey())) {
					importMappings.put(schemaEntry.getKey(), schemaEntry.getValue());
				}
			});
		}

		return importMappings;
	}

	/**
	 * Hook in this method to the preprocessOpenAPI() method and you get the features of
	 * {@link IKeytilesCodegen#OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING},
	 * {@link IKeytilesCodegen#OPT_EXCLUDE_IMPORT_MAPPINGS_FROM_GENERATION},
	 * {@link IKeytilesCodegen#OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS} and support
	 * for registering {@link MavenExecutionInfo} if {@link IKeytilesCodegen#OPT_MAVEN_EXECUTION_ID} is
	 * set
	 *
	 * @param codegen
	 *            your codegen module
	 * @param openAPI
	 *            the param from the hook method
	 */
	public static void preprocessOpenAPIHook(IKeytilesCodegen codegen, OpenAPI openAPI) {

		// first deal with importMappings!

		if (codegen.getAddSchemaModelsToImportMappings() != null) {
			LOGGER.info("option '{}' is set - scanning for imports...",
					IKeytilesCodegen.OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING);

			Map<String, String> scannedImportMappings = ConfigOptionHelper
					.getImportMappingsToAdd(codegen.getAddSchemaModelsToImportMappings());
			codegen.importMapping().putAll(scannedImportMappings);

			LOGGER.info("import scan complete! The following imports will be added: {}", scannedImportMappings);
		}

		if (codegen.getAddSchemaModelsToImportMappingsFromMavenExecutions() != null) {
			LOGGER.info("option '{}' is set - scanning for imports...",
					IKeytilesCodegen.OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS);

			Map<String, String> scannedImportMappings = ConfigOptionHelper.getImportMappingsFromMavenExecutions(
					codegen.getAddSchemaModelsToImportMappingsFromMavenExecutions());
			codegen.importMapping().putAll(scannedImportMappings);

			LOGGER.info("import scan complete! The following imports will be added: {}", scannedImportMappings);
		}

		// now we can create the Maven execution info object
		// as now we know our importSet

		if (codegen.getMavenExecutionId() != null) {
			MavenExecutionInfo executionInfo = MavenExecutionInfo.createExecutionInfo(codegen);
			executionInfo.setImportMappings(codegen.importMapping());
		}

		// finally some excludes if needed

		if (codegen.excludeImportMappingsFromGeneration()) {
			LOGGER.info("option '{}' is set - scanning schemas...",
					IKeytilesCodegen.OPT_EXCLUDE_IMPORT_MAPPINGS_FROM_GENERATION);
			Map<String, Schema> schemas = openAPI.getComponents().getSchemas();
			Set<String> excludeModelsFromGeneration = new HashSet<>();
			schemas.entrySet().forEach(schemaEntry -> {
				if (codegen.importMapping().containsKey(schemaEntry.getKey())) {
					excludeModelsFromGeneration.add(schemaEntry.getKey());
					LOGGER.info("model {} is excluded from generation - found in importMappings", schemaEntry.getKey());
				}
			});
			// let the codegen know about these exclusions!
			codegen.setExcludeModelsFromGeneration(excludeModelsFromGeneration);
		}
	}

}
