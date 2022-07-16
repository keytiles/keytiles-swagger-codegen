package com.keytiles.swagger.codegen.helper.debug;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.keytiles.swagger.codegen.IKeytilesCodegen;

import io.swagger.codegen.v3.CodegenProperty;

/**
 * This class is used when {@link IKeytilesCodegen#OPT_ADD_EXPLANATIONS_TO_MODEL} option us turned
 * on
 * <p>
 * The class is a simple placeholder for extra information belongs to the property. As you can see
 * follows a static method model so it is easy to use it from everywhere in the code
 * <p>
 * It simply hooks in to {@link CodegenProperty} instances - using
 * {@link CodegenProperty#getVendorExtensions()} part
 *
 * @author attil
 *
 */
public class PropertyExplanations {

	public final static String X_PROPERTY_EXPLANATIONS = "x-keytiles-property-explanations";

	/**
	 * This method returns the {@link PropertyExplanations} instance from the given property - if
	 * already exists. If not exists then will create and attach it
	 */
	public static PropertyExplanations getOrCreateExplanations(CodegenProperty property) {
		PropertyExplanations explanations = (PropertyExplanations) property.vendorExtensions
				.get(X_PROPERTY_EXPLANATIONS);
		if (explanations == null) {
			explanations = new PropertyExplanations();
			property.vendorExtensions.put(X_PROPERTY_EXPLANATIONS, explanations);
		}
		return explanations;
	}

	/**
	 * This method returns the {@link PropertyExplanations} instance from the given property - if
	 * exists. Otherwise it returns NULL
	 */
	public static PropertyExplanations getExplanations(CodegenProperty property) {
		return (PropertyExplanations) property.vendorExtensions.get(X_PROPERTY_EXPLANATIONS);
	}

	/**
	 * You can append a new explanation message to the messages belong to the property field itself.
	 * <p>
	 * note: this is just done if the property has an already registered {@link PropertyExplanations}
	 * instance in it otherwise this method does nothing
	 *
	 * @param property
	 *            which property?
	 * @param message
	 *            your property field explanation message
	 */
	public static void appendToProperty(CodegenProperty property, String message) {
		if (getExplanations(property) != null) {
			getExplanations(property).appendToProperty(message);
		}
	}

	/**
	 * You can append a new explanation message to the messages belong to the property field getter
	 * method.
	 * <p>
	 * note: this is just done if the property has an already registered {@link PropertyExplanations}
	 * instance in it otherwise this method does nothing
	 *
	 * @param property
	 *            which property?
	 * @param message
	 *            your property getter method explanation message
	 */
	public static void appendToGetter(CodegenProperty property, String message) {
		if (getExplanations(property) != null) {
			getExplanations(property).appendToGetter(message);
		}
	}

	/**
	 * You can append a new explanation message to the messages belong to the property field setter
	 * method.
	 * <p>
	 * note: this is just done if the property has an already registered {@link PropertyExplanations}
	 * instance in it otherwise this method does nothing
	 *
	 * @param property
	 *            which property?
	 * @param message
	 *            your property setter method explanation message
	 */
	public static void appendToSetter(CodegenProperty property, String message) {
		if (getExplanations(property) != null) {
			getExplanations(property).appendToSetter(message);
		}
	}

	private final List<Map<String, String>> forProperty = new LinkedList<>();
	private final List<Map<String, String>> forGetter = new LinkedList<>();
	private final List<Map<String, String>> forSetter = new LinkedList<>();

	public PropertyExplanations() {
	}

	public List<Map<String, String>> getForProperty() {
		return forProperty;
	}

	public List<Map<String, String>> getForGetter() {
		return forGetter;
	}

	public List<Map<String, String>> getForSetter() {
		return forSetter;
	}

	private void appendToProperty(String message) {
		Map<String, String> wrapper = new HashMap<>();
		wrapper.put("explanationMessage", message);

		forProperty.add(wrapper);
	}

	private void appendToGetter(String message) {
		Map<String, String> wrapper = new HashMap<>();
		wrapper.put("explanationMessage", message);

		forGetter.add(wrapper);
	}

	private void appendToSetter(String message) {
		Map<String, String> wrapper = new HashMap<>();
		wrapper.put("explanationMessage", message);

		forSetter.add(wrapper);
	}

}
