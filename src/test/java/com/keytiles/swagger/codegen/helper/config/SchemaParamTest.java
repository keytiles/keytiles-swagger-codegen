package com.keytiles.swagger.codegen.helper.config;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import com.keytiles.swagger.codegen.ResourceUtil;

public class SchemaParamTest {

	@Test
	public void schemaParsingTest() {

		Pattern refPattern = Pattern.compile("^.*\\$ref.*\\/components\\/schemas\\/(?<modelName>[^'\"]*)");

		boolean qqq = false;

		// qqq = refPattern.matcher("")

		// ---- GIVEN

		String schemaYamlFile = ResourceUtil
				.getRealFilesystemPathForResource("test/openapi/imported-types-2nd-level.yaml");
		String paramDef = schemaYamlFile + "::modelPackage = com.keytiles.test";

		// ---- WHEN

		SchemaParam schemaParam = SchemaParam.fromFlatStringDefinition("configOptionName", paramDef);

		// ---- THEN

		Assert.assertNotNull(schemaParam.getOpenAPI());
		Assert.assertEquals("com.keytiles.test", schemaParam.getModelPackage());
	}
}
