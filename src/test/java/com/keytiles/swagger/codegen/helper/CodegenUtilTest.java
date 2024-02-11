package com.keytiles.swagger.codegen.helper;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Preconditions;
import com.keytiles.swagger.codegen.KeytilesJavaCodegen;
import com.keytiles.swagger.codegen.testing.GeneratorForTests;
import com.keytiles.swagger.codegen.testing.TestHelper;

import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;

public class CodegenUtilTest {

	private GeneratorForTests generator = null;

	@After
	public void cleanupAfterTestCase() {
		if (generator != null) {
			generator.deleteAllGeneratedFiles();
			generator.deleteOutputFolder();
			generator = null;
		}
	}

	private Map<String, CodegenModel> getModelsGeneratedByKeytilesCodegen(String schemaFileResourcePath,
			String outputTempFolder, @Nullable Map<String, Object> generatorOptions) {
		Preconditions.checkState(generator == null, "Hey! You have created a generator already!");

		generator = TestHelper.createCodegenGenerator(CodegenUtilTest.class, schemaFileResourcePath,
				KeytilesJavaCodegen.class, outputTempFolder, generatorOptions);
		generator.generate();
		Map<String, CodegenModel> allModels = generator.getCodegen().getAllModels();

		return allModels;
	}

	private CodegenProperty getProperty(CodegenModel model, String propertyName) {
		CodegenProperty prop = CodegenUtil.getPropertyByName(model, propertyName);
		Preconditions.checkArgument(prop != null, "Oops! There is no property named '%s' in model '%s'", propertyName,
				model.name);
		return prop;
	}

	private void assumeAssignable(CodegenModel modelA, CodegenModel modelB, String propAName, String propBName,
			Collection<CodegenModel> allModels) {

		CodegenProperty propA = getProperty(modelA, propAName);
		CodegenProperty propB = getProperty(modelB, propBName);

		Assert.assertTrue(
				"it looks " + modelA.name + "." + propAName + " (type " + propA.datatype + ") is not assignable from "
						+ modelB.name + "." + propBName + " (type " + propB.datatype + ") - however it should be...",
				CodegenUtil.isPropertyAssignableFromProperty(propA, propB, allModels));
	}

	private void assumeNotAssignable(CodegenModel modelA, CodegenModel modelB, String propAName, String propBName,
			Collection<CodegenModel> allModels) {

		CodegenProperty propA = getProperty(modelA, propAName);
		CodegenProperty propB = getProperty(modelB, propBName);

		Assert.assertFalse(
				"it looks " + modelA.name + "." + propAName + " (type " + propA.datatype + ") is assignable from "
						+ modelB.name + "." + propBName + " (type " + propB.datatype
						+ ") - however it should NOT be...",
				CodegenUtil.isPropertyAssignableFromProperty(propA, propB, allModels));
	}

	@Test
	public void isModelIsAssignableFromModelTest() {

		// ---- GIVEN

		String schemaFileResourcePath = "test/openapi/CodegenUtilTest/schema1.yaml";
		Map<String, CodegenModel> allModels = getModelsGeneratedByKeytilesCodegen(schemaFileResourcePath,
				CodegenUtilTest.class.getSimpleName() + "_isModelIsAssignableFromModelTest", null);
		CodegenModel fieldClassModel = allModels.get("FieldClass");
		CodegenModel extendedFieldClassModel = allModels.get("ExtendedFieldClass");
		CodegenModel furtherExtendedFieldClassModel = allModels.get("FurtherExtendedFieldClass");

		// ---- WHEN & THEN

		// identity check
		Assert.assertTrue(CodegenUtil.isModelAssignableFromModel(fieldClassModel, fieldClassModel));

		// 1 level check
		Assert.assertTrue(CodegenUtil.isModelAssignableFromModel(fieldClassModel, extendedFieldClassModel));

		// transitivity check
		Assert.assertTrue(CodegenUtil.isModelAssignableFromModel(fieldClassModel, furtherExtendedFieldClassModel));

	}

	@Test
	public void isPropertyAssignableFromPropertyTest() {

		// ---- GIVEN

		String schemaFileResourcePath = "test/openapi/CodegenUtilTest/schema1.yaml";
		Map<String, CodegenModel> allModels = getModelsGeneratedByKeytilesCodegen(schemaFileResourcePath,
				CodegenUtilTest.class.getSimpleName() + "_isPropertyAssignableFromPropertyTest", null);
		CodegenModel classAModel = allModels.get("ClassA");
		CodegenModel classBModel = allModels.get("ClassB");

		// ---- WHEN & THEN

		// every property should be assignable from it's pair in ClassA and ClassB
		// BUT
		// and every property other than it's pair should NOT be assignable from any other ClassB
		// EXCEPT
		// if property name says it is its extension

		for (CodegenProperty propA : classAModel.vars) {
			for (CodegenProperty propB : classBModel.vars) {
				if (propA.baseName.equals(propB.baseName)) {
					// it is the pair of it - should work
					assumeAssignable(classAModel, classBModel, propA.baseName, propB.baseName, allModels.values());
					// also should work other way around
					assumeAssignable(classAModel, classBModel, propB.baseName, propA.baseName, allModels.values());
				} else {
					// we have some special fields which "extends" the other regarding type
					if (propB.baseName.toLowerCase().equals("extended" + propA.baseName.toLowerCase())) {
						// should work this way
						assumeAssignable(classAModel, classBModel, propA.baseName, propB.baseName, allModels.values());
						// but NOT the other way around
						assumeNotAssignable(classAModel, classBModel, propB.baseName, propA.baseName,
								allModels.values());
					} else {

						// and now we have to totally different fields at hand - should NOT work
						assumeNotAssignable(classAModel, classBModel, propA.baseName, propB.baseName,
								allModels.values());
					}
				}
			}
		}

	}
}
