package com.keytiles.swagger.codegen.model;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.KeytilesJavaCodegen;
import com.keytiles.swagger.codegen.error.SchemaValidationException;
import com.keytiles.swagger.codegen.testing.GeneratorForTests;
import com.keytiles.swagger.codegen.testing.TestHelper;

import io.swagger.codegen.v3.CodegenModel;

public class BuildFailsTest {

	private GeneratorForTests generator = null;

	@After
	public void cleanupAfterTestCase() {
		if (generator != null) {
			generator.deleteAllGeneratedFiles();
			generator.deleteOutputFolder();
			generator = null;
		}

		ModelExtraInfo.cleanStaticExtraInfoCache();
	}

	private void generateModel(String schemaFileResourcePath, String outputTempFolder,
			@Nullable Map<String, Object> generatorOptions) {
		Preconditions.checkState(generator == null, "Hey! You have created a generator already!");

		generator = TestHelper.createCodegenGenerator(BuildFailsTest.class, schemaFileResourcePath,
				KeytilesJavaCodegen.class, outputTempFolder, generatorOptions);
		generator.generate();
		Map<String, CodegenModel> allModels = generator.getCodegen().getAllModels();
	}

	private Exception runFailureCase(String methodName, String schemaFileName) {

		cleanupAfterTestCase();

		// ---- GIVEN

		String schemaFileResourcePath = "test/openapi/ModelExtraInfoTest/" + schemaFileName;
		Map<String, Object> generatorOptions = new HashMap<>();
		generatorOptions.put(IKeytilesCodegen.OPT_ADD_EXPLANATIONS_TO_MODEL, "true");
		generatorOptions.put(IKeytilesCodegen.OPT_MODEL_STYLE, ModelStyle.simpleConsistent.toString());

		// ---- WHEN

		Exception exceptionThrown = null;
		try {
			generateModel(schemaFileResourcePath, BuildFailsTest.class.getSimpleName() + "_failureCases",
					generatorOptions);
		} catch (Exception e) {
			exceptionThrown = e;
		}

		// ---- THEN

		Assert.assertNotNull(exceptionThrown);
		System.out.println("======= in method '" + methodName + "' exception was thrown:");
		exceptionThrown.printStackTrace();
		System.out.println("\n======================\n");

		Assert.assertTrue(exceptionThrown instanceof SchemaValidationException);
		// make sure we "routed" the user to the README file
		Assert.assertTrue(exceptionThrown.getMessage().contains("README"));
		Assert.assertTrue(exceptionThrown.getMessage().contains("Java limitations with generating models"));

		return exceptionThrown;
	}

	@Test
	public void javaLimitations_failureCases() {

		runFailureCase("javaLimitations_failureCase1", "failure-case1.yaml");
		runFailureCase("javaLimitations_failureCase1b", "failure-case1b.yaml");
		runFailureCase("javaLimitations_failureCase2", "failure-case2.yaml");
		runFailureCase("javaLimitations_failureCase2b", "failure-case2b.yaml");
		runFailureCase("javaLimitations_failureCase3", "failure-case3.yaml");
		runFailureCase("javaLimitations_failureCase4", "failure-case4.yaml");
		runFailureCase("javaLimitations_failureCase5", "failure-case5.yaml");
		runFailureCase("javaLimitations_failureCase6", "failure-case6.yaml");
		runFailureCase("javaLimitations_failureCase7", "failure-case7.yaml");

	}

}
