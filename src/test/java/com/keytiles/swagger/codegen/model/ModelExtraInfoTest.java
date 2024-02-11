package com.keytiles.swagger.codegen.model;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.IKeytilesCodegen.ModelState;

import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;

public class ModelExtraInfoTest {

	private CodegenModel getModelWithProperty(boolean nullable, boolean hasDefault, boolean mandatory, boolean readonly)
			throws JsonProcessingException {
		CodegenModel model = new CodegenModel();
		model.vendorExtensions.put(IKeytilesCodegen.X_MODEL_STATE, ModelState.fullyEnriched);

		model.name = "theModel";
		CodegenProperty property = new CodegenProperty();
		property.baseName = "baseName";
		property.name = "name";
		model.vars.add(property);

		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "string");

		property.nullable = nullable;
		property.vendorExtensions.put(CodegenConstants.IS_NULLABLE_EXT_NAME, nullable);
		schema.put("nullable", nullable);

		if (hasDefault) {
			property.defaultValue = "defaultValue";
			schema.put("default", property.defaultValue);
		} else {
			property.defaultValue = "null";
		}

		if (readonly) {
			schema.put("readOnly", true);
			property.vendorExtensions.put(CodegenConstants.IS_READ_ONLY_EXT_NAME, true);
		}

		if (mandatory) {
			model.mandatory.add(property.baseName);
		}

		ObjectMapper mapper = new ObjectMapper();
		property.jsonSchema = mapper.writeValueAsString(schema);

		return model;
	}

	/**
	 * Check the attached excel sheet for all cases!
	 *
	 * @throws JsonProcessingException
	 */
	@Test
	public void RuleSetTest() throws JsonProcessingException {

		CodegenModel model = null;
		ModelExtraInfo extraInfo = null;
		IKeytilesCodegen codeGenerator = null; // we can leave this on null
		Exception exceptionThrown = null;

		// ---- GIVEN
		model = getModelWithProperty(false, false, false, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().isEmpty());
		Assert.assertTrue(extraInfo.getPrivateFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().contains(model.vars.get(0)));

		// ---- GIVEN
		model = getModelWithProperty(true, false, false, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().isEmpty());
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().contains(model.vars.get(0)));
		Assert.assertTrue(!extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().isEmpty());
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(false, true, false, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().isEmpty());
		Assert.assertTrue(extraInfo.getPrivateFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(!extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().isEmpty());
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(true, true, false, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().isEmpty());
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().contains(model.vars.get(0)));
		Assert.assertTrue(!extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().isEmpty());
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(false, false, true, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().isEmpty());
		Assert.assertTrue(extraInfo.getPrivateFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().contains(model.vars.get(0)));

		// ---- GIVEN
		model = getModelWithProperty(true, false, true, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().isEmpty());
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().isEmpty());
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().contains(model.vars.get(0)));

		// ---- GIVEN
		model = getModelWithProperty(false, true, true, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNotNull(exceptionThrown);
		Assert.assertTrue(exceptionThrown.getMessage()
				.contains("can not be 'required=true' while having 'default: <value>' at the same time"));

		// ---- GIVEN
		model = getModelWithProperty(true, true, true, false);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNotNull(exceptionThrown);
		Assert.assertTrue(exceptionThrown.getMessage()
				.contains("can not be 'required=true' while having 'default: <value>' at the same time"));

		// ---- GIVEN
		model = getModelWithProperty(false, false, false, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(true, false, false, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().isEmpty());
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(false, true, false, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(true, true, false, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().isEmpty());
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(false, false, true, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());

		// ---- GIVEN
		model = getModelWithProperty(true, false, true, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNull(exceptionThrown);
		Assert.assertTrue(extraInfo.getPrivateFinalFields().contains(model.vars.get(0)));
		Assert.assertTrue(extraInfo.getPrivateFields().isEmpty());
		Assert.assertTrue(extraInfo.getPublicFields().isEmpty());
		Assert.assertTrue(extraInfo.needsConstructor());
		Assert.assertTrue(extraInfo.getCtorValidateNonNullValueArguments().isEmpty());
		Assert.assertTrue(extraInfo.getCtorOwnFieldArguments().isEmpty());
		// ---- GIVEN
		model = getModelWithProperty(false, true, true, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNotNull(exceptionThrown);
		Assert.assertTrue(exceptionThrown.getMessage()
				.contains("can not be 'required=true' while having 'default: <value>' at the same time"));

		// ---- GIVEN
		model = getModelWithProperty(true, true, true, true);
		exceptionThrown = null;
		// ---- WHEN
		try {
			extraInfo = new ModelExtraInfo(model, codeGenerator);
		} catch (Exception e) {
			exceptionThrown = e;
		}
		// ---- THEN
		Assert.assertNotNull(exceptionThrown);
		Assert.assertTrue(exceptionThrown.getMessage()
				.contains("can not be 'required=true' while having 'default: <value>' at the same time"));

	}

}
