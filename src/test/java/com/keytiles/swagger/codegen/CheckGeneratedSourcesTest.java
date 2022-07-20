package com.keytiles.swagger.codegen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.keytiles.api.model.test.simpleconsistent.NonNullableFieldsClass;
import com.keytiles.api.model.test.simpleconsistent.NonNullableFieldsClass.InlineEnumFieldEnum;
import com.keytiles.api.model.test.simpleconsistent.NonNullableFieldsClassInlineLangObjectField;
import com.keytiles.api.model.test.simpleconsistent.imported.NonNullablePrimeEnum;
import com.keytiles.api.model.test.simpleconsistent.ref_attribute_inheritance.ReferredNullableEnumWithDefault;
import com.keytiles.api.model.test.simpleconsistent.ref_attribute_inheritance.ReferredNullableObject;
import com.keytiles.api.model.test.simpleconsistent.ref_attribute_inheritance.ReferredObject;
import com.keytiles.api.model.test.simpleconsistent.ref_attribute_inheritance.ReferringClass;

public class CheckGeneratedSourcesTest {

	private Map<String, Field> getDeclaredFields(Class<?> theClass) {
		Map<String, Field> fields = new HashMap<>();
		for (Field field : theClass.getDeclaredFields()) {
			fields.put(field.getName(), field);
		}
		return fields;
	}

	public CheckGeneratedSourcesTest() {

		// TODO this is crap, add much more in a logical structure!

		Long longField = 5l;
		Double doubleField = 5d;
		InlineEnumFieldEnum inlineEnumField = InlineEnumFieldEnum.OK;
		NonNullablePrimeEnum primeEnumField = NonNullablePrimeEnum.NUMBER_11;
		List<String> arrayField = new ArrayList<>();
		NonNullableFieldsClassInlineLangObjectField inlineLangObjectField = new NonNullableFieldsClassInlineLangObjectField(
				5, "langcode", "label");

		NonNullableFieldsClass nonNullableFieldsClass = new NonNullableFieldsClass(longField, doubleField,
				inlineEnumField, primeEnumField, arrayField, inlineLangObjectField);

		Assert.assertEquals(new ArrayList<>(Arrays.asList("a", "b")),
				nonNullableFieldsClass.getArrayFieldWithDefault());
	}

	/**
	 * Testing classes generated from /src/test/openapi/ref-attribute-inheritance.yaml
	 */
	@Test
	public void refAttributeInheritanceGeneratedSourcesTest() {

		// ---- GIVEN

		ReferringClass theInstance = new ReferringClass();
		Class<ReferringClass> theClass = ReferringClass.class;
		Map<String, Field> fields = getDeclaredFields(theClass);

		// ---- THEN - referredNullableEnumField

		Assert.assertTrue(fields.containsKey("referredNullableEnumField"));
		// it should be public field
		Assert.assertTrue(Modifier.isPublic(fields.get("referredNullableEnumField").getModifiers()));
		// type should be this
		Assert.assertEquals(ReferredNullableEnumWithDefault.class, fields.get("referredNullableEnumField").getType());
		// enum has a default value in schema - we should see that default value
		Assert.assertEquals(ReferredNullableEnumWithDefault.ORANGE, theInstance.referredNullableEnumField);

		// ---- THEN - referredNullableObjectField

		Assert.assertTrue(fields.containsKey("referredNullableObjectField"));
		// it should be public field
		Assert.assertTrue(Modifier.isPublic(fields.get("referredNullableObjectField").getModifiers()));
		// type should be this
		Assert.assertEquals(ReferredNullableObject.class, fields.get("referredNullableObjectField").getType());
		// enum has a default value in schema - we should see that default value
		Assert.assertNull(theInstance.referredNullableObjectField);

		// ---- THEN - inPlaceNullableObjectField

		Assert.assertTrue(fields.containsKey("inPlaceNullableObjectField"));
		// it should be public field
		Assert.assertTrue(Modifier.isPublic(fields.get("inPlaceNullableObjectField").getModifiers()));
		// the initial - as nullable - should be null
		Assert.assertNull(theInstance.referredNullableObjectField);
		// but we have to be able to pass in a ReferredObject into it
		ReferredObject testObj = new ReferredObject();
		testObj.prop1 = "testProp";
		theInstance.inPlaceNullableObjectField = testObj;
	}

}
