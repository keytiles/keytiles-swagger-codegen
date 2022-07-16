package com.keytiles.swagger.codegen.helper.maven;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.keytiles.swagger.codegen.IKeytilesCodegen;

import io.swagger.codegen.v3.CodegenModel;

/**
 * If you are using the Maven generator plugin then this class is registering information about each
 * <execution> you add to the pom.xml
 *
 * The advantage of this is making the support of
 * {@link IKeytilesCodegen#OPT_ADD_SCHEMA_MODELS_TO_IMPORT_MAPPING_FROM_MAVENEXECUTIONS} option
 * possible
 *
 * @author attil
 *
 */
public class MavenExecutionInfo {

	private final static Map<String, MavenExecutionInfo> executions = new HashMap<>();

	public static MavenExecutionInfo getExecutionInfo(String executionId) {
		return executions.get(executionId);
	}

	/**
	 * Creates a MavenExecutionInfo for the provided generator.
	 * <p>
	 * The generator must have a {@link IKeytilesCodegen#getMavenExecutionId()} and a
	 * {@link IKeytilesCodegen#modelPackage()} set otherwise you will get exception
	 *
	 * @return
	 */
	public static MavenExecutionInfo createExecutionInfo(IKeytilesCodegen fromCodegen) {
		Preconditions.checkArgument(fromCodegen != null, "'fromCodegen' param can not be NULL");
		Preconditions.checkArgument(fromCodegen.getMavenExecutionId() != null,
				"The provided 'fromCodegen' (inputSpec %s) does not have a Maven executionID set - which is mandatory to track this execution!",
				fromCodegen.getInputSpec());
		Preconditions.checkArgument(StringUtils.isNotBlank(fromCodegen.modelPackage()),
				"The provided 'fromCodegen' (inputSpec %s) does not have modelPackage set - which is mandatory to track this execution!",
				fromCodegen.getInputSpec());

		MavenExecutionInfo executionInfo = new MavenExecutionInfo(fromCodegen.getMavenExecutionId(),
				fromCodegen.modelPackage());

		return executionInfo;
	}

	// the id
	private final String executionId;
	// the value of <modelPackage> option for this execution
	private final String modelPackage;
	// the models which were put together eventually during the execution
	private final Map<String, CodegenModel> models;
	// the assembled importMappings which was used in this execution
	private Map<String, String> importMappings;

	private MavenExecutionInfo(String executionId, String modelPackage) {
		this.executionId = executionId;
		this.models = new HashMap<>();
		this.modelPackage = modelPackage;

		// let's register
		executions.put(executionId, this);
	}

	public Map<String, String> getImportMappings() {
		return importMappings;
	}

	public void setImportMappings(Map<String, String> importMappings) {
		// we take it as it is! by reference, intentionally
		this.importMappings = importMappings;
	}

	public String getExecutionId() {
		return executionId;
	}

	public String getModelPackage() {
		return modelPackage;
	}

	public Map<String, CodegenModel> getModels() {
		return models;
	}

	public void registerModel(String modelName, CodegenModel model) {
		models.put(modelName, model);
	}

	public Map<String, String> getModelsForImportMapping(boolean addOwnImportMappingsToo) {
		Map<String, String> mappings = new HashMap<>();

		if (addOwnImportMappingsToo && importMappings != null) {
			mappings.putAll(importMappings);
		}

		models.entrySet().forEach(modelEntry -> {
			mappings.put(modelEntry.getKey(), modelPackage + "." + modelEntry.getValue().name);
		});

		return mappings;
	}
}
