package com.keytiles.swagger.codegen.testing;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.keytiles.swagger.codegen.IKeytilesCodegen;

import io.swagger.codegen.v3.ClientOptInput;
import io.swagger.codegen.v3.Generator;
import io.swagger.codegen.v3.config.CodegenConfigurator;

public class TestHelper {

	private TestHelper() {
	}

	/**
	 * Method prepares a {@link Generator} for you which will use the given {@link IKeytilesCodegen}
	 * under the hood generating from the given OpenApi schema file
	 *
	 * @param <T>
	 *            which Codegen class you want to use?
	 * @param schemaFileResourcePath
	 *            the OpenApi schema file path under resources
	 * @param codegenClass
	 *            which codegen class you want to use
	 * @param outputTempFolder
	 *            Where to save results of generation as an output - this folder will be created under
	 *            /target/test-classes/test-resources
	 * @param generatorOptions
	 *            Optionally you can pass a map of options - see {@link IKeytilesCodegen}.OPT_xxx
	 *            constants
	 * @return the initiated generator
	 */
	public static GeneratorForTests createCodegenGenerator(Class<?> forTestClass, String schemaFileResourcePath,
			Class<? extends IKeytilesCodegen> codegenClass, String outputTempFolder,
			@Nullable Map<String, Object> generatorOptions) {

		// let's read up the schema
		String openApiFileContent = ResourceUtil.loadResourceTextFileContent(schemaFileResourcePath);

		CodegenConfigurator configurator = new CodegenConfigurator();
		configurator.setInputSpec(openApiFileContent);
		configurator.setLang(codegenClass.getName());
		Map<String, Object> defaultOptions = new HashMap<>();
		defaultOptions.put("generateApiTests", "false");
		defaultOptions.put("generateApiDocs", "false");
		defaultOptions.put("generateApis", "false");
		defaultOptions.put("generateModels", "true");
		defaultOptions.put("generateApiDocumentation", "false");
		defaultOptions.put("generateModelTests", "false");
		defaultOptions.put("generateModelDocumentation", "false");
		defaultOptions.put("generateSupportingFiles", "false");
		configurator.getAdditionalProperties().putAll(defaultOptions);
		if (generatorOptions != null) {
			configurator.getAdditionalProperties().putAll(generatorOptions);
		}

		// let's create an output temp dir for the generator!
		String baseDir = ResourceUtil.getRealFilesystemPathForResource("test-resources/temp-models/readme.txt");
		baseDir = new File(baseDir).getParent();
		File tempFolder = new File(baseDir, outputTempFolder);
		tempFolder.mkdir();

		configurator.setOutputDir(tempFolder.getAbsolutePath());
		ClientOptInput optInput = configurator.toClientOptInput();
		GeneratorForTests generator = (GeneratorForTests) new GeneratorForTests(forTestClass).opts(optInput);

		return generator;
	}

}
