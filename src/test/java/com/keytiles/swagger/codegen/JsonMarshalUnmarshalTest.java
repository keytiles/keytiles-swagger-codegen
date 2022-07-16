package com.keytiles.swagger.codegen;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keytiles.api.model.test.JsonSerializationTestSubclassClass;

public class JsonMarshalUnmarshalTest {

	/**
	 * This object mapper is less restrictive with the JSON. It allows the presence of extra elements
	 * which will be silently ignored
	 */
	public final static ObjectMapper mapperAllowsExtraElements = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	/**
	 * This object mapper is a strict one - it does not allow extra elements in the JSON at all.
	 * Presence of extra elements will result in an exception during unmarshal
	 */
	public final static ObjectMapper defaultStrictMapper = new ObjectMapper();

	/**
	 * This object mapper is less restrictive with the JSON. It allows the presence of extra elements
	 * which will be silently ignored
	 */
	public final static ObjectMapper mapperAllowsExtraElementsBigDecimalsString = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

	/**
	 * This object mapper is a strict one - it does not allow extra elements in the JSON at all.
	 * Presence of extra elements will result in an exception during unmarshal
	 */
	public final static ObjectMapper defaultStrictMapperBigDecimalsString = new ObjectMapper()
			.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

	@Test
	public void serializeToDeserializeFromJsonStringTest() throws JsonProcessingException {

		// ---- GIVEN

		int requiredUnconventionalNameBaseField = 10;
		String requiredOutputIfNonDefaultBaseString = "requiredOutputIfNonDefaultBaseStringValue"; // give it a value
																									// now
		int nonNullNoDefaultBaseInteger = 20;
		int nonNullBaseInteger = 30;
		String baseStringField = "baseString";

		boolean requiredBooleanField = true;
		double nonNullNoDefaultDouble = 12.8d;
		int nonNullInteger = 40;
		long longField = 1000000l;
		int unconventionalNameField = 50;

		JsonSerializationTestSubclassClass testObj = new JsonSerializationTestSubclassClass(
				requiredUnconventionalNameBaseField, requiredOutputIfNonDefaultBaseString, nonNullNoDefaultBaseInteger,
				requiredBooleanField, nonNullNoDefaultDouble);
		testObj.setNonNullBaseInteger(nonNullBaseInteger);
		testObj.setNonNullInteger(nonNullInteger);
		testObj.baseStringField = baseStringField;
		testObj.longField = longField;
		testObj.unconventionalNameField = unconventionalNameField;

		// ---- WHEN

		String jsonString = defaultStrictMapperBigDecimalsString.writeValueAsString(testObj);

		// ---- THEN

		Assert.assertTrue(jsonString.contains("\"required_unconventional_name_baseField\":10"));
		Assert.assertTrue(jsonString
				.contains("\"requiredOutputIfNonDefaultBaseString\":\"requiredOutputIfNonDefaultBaseStringValue\""));
		Assert.assertTrue(jsonString.contains("\"nonNullNoDefaultBaseInteger\":20"));
		Assert.assertTrue(jsonString.contains("\"requiredBooleanField\":true"));
		Assert.assertTrue(jsonString.contains("\"nonNullNoDefaultDouble\":12.8"));
		Assert.assertTrue(jsonString.contains("\"nonNullBaseInteger\":30"));
		Assert.assertTrue(jsonString.contains("\"baseStringField\":\"baseString\""));
		Assert.assertTrue(jsonString.contains("\"nonNullInteger\":40"));
		Assert.assertTrue(jsonString.contains("\"unconventional_name_Field\":50"));
		Assert.assertTrue(jsonString.contains("\"longField\":1000000"));

		// now time to deserialize!

		// ---- WHEN

		JsonSerializationTestSubclassClass deserializedObj = defaultStrictMapperBigDecimalsString.readValue(jsonString,
				JsonSerializationTestSubclassClass.class);

		// ---- THEN

		Assert.assertEquals(requiredUnconventionalNameBaseField,
				deserializedObj.getRequiredUnconventionalNameBaseField());
		Assert.assertEquals(nonNullNoDefaultBaseInteger, deserializedObj.getNonNullNoDefaultBaseInteger().intValue());
		Assert.assertEquals(nonNullBaseInteger, deserializedObj.getNonNullBaseInteger().intValue());
		Assert.assertEquals(requiredOutputIfNonDefaultBaseString,
				deserializedObj.getRequiredOutputIfNonDefaultBaseString());
		Assert.assertEquals(baseStringField, deserializedObj.baseStringField);
		Assert.assertEquals(requiredBooleanField, deserializedObj.isRequiredBooleanField());
		Assert.assertEquals(nonNullNoDefaultDouble, deserializedObj.getNonNullNoDefaultDouble().doubleValue(),
				0.0000001d);
		Assert.assertEquals(nonNullInteger, deserializedObj.getNonNullInteger().intValue());
		Assert.assertEquals(longField, deserializedObj.longField);
		Assert.assertEquals(unconventionalNameField, deserializedObj.unconventionalNameField);

		// finally, let's test the equals method!

		Assert.assertEquals(testObj, deserializedObj);

	}

	@Test
	public void serializeOutputOnlyNonDefaultFieldsTest() throws JsonProcessingException {

		int requiredUnconventionalNameBaseField = 10;
		int nonNullNoDefaultBaseInteger = 20;
		boolean requiredBooleanField = true;
		double nonNullNoDefaultDouble = 12.8d;

		/*
		 * Scenario 1
		 * fields left/set on their default value - they should not appear in Json string
		 */

		// ---- GIVEN

		String requiredOutputIfNonDefaultBaseString = null;
		int outputIfNonDefaultBaseInteger = 0;
		String outputIfNonDefaultString = null;

		JsonSerializationTestSubclassClass testObj = new JsonSerializationTestSubclassClass(
				requiredUnconventionalNameBaseField, requiredOutputIfNonDefaultBaseString, nonNullNoDefaultBaseInteger,
				requiredBooleanField, nonNullNoDefaultDouble);
		testObj.outputIfNonDefaultBaseInteger = outputIfNonDefaultBaseInteger;
		testObj.outputIfNonDefaultString = outputIfNonDefaultString;

		// ---- WHEN

		String jsonString = defaultStrictMapperBigDecimalsString.writeValueAsString(testObj);

		// ---- THEN

		Assert.assertTrue(!jsonString.contains("\"requiredOutputIfNonDefaultBaseString\""));
		Assert.assertTrue(!jsonString.contains("\"outputIfNonDefaultBaseInteger\""));
		Assert.assertTrue(!jsonString.contains("\"outputIfNonDefaultString\""));

		/*
		 * Scenario 2
		 * now we set fields to non-default value - they should appear in Json string
		 */

		// ---- GIVEN

		requiredOutputIfNonDefaultBaseString = "buuu";
		outputIfNonDefaultBaseInteger = 5;
		outputIfNonDefaultString = "baaa";

		testObj = new JsonSerializationTestSubclassClass(requiredUnconventionalNameBaseField,
				requiredOutputIfNonDefaultBaseString, nonNullNoDefaultBaseInteger, requiredBooleanField,
				nonNullNoDefaultDouble);
		testObj.outputIfNonDefaultBaseInteger = outputIfNonDefaultBaseInteger;
		testObj.outputIfNonDefaultString = outputIfNonDefaultString;

		// ---- WHEN

		jsonString = defaultStrictMapperBigDecimalsString.writeValueAsString(testObj);

		// ---- THEN

		Assert.assertTrue(jsonString.contains("\"requiredOutputIfNonDefaultBaseString\":\"buuu\""));
		Assert.assertTrue(jsonString.contains("\"outputIfNonDefaultBaseInteger\":5"));
		Assert.assertTrue(jsonString.contains("\"outputIfNonDefaultString\":\"baaa\""));

	}

}
