package com.keytiles.swagger.codegen.helper.debug;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.keytiles.swagger.codegen.IKeytilesCodegen;

import io.swagger.codegen.v3.CodegenModel;

/**
 * This class is used when {@link IKeytilesCodegen#OPT_ADD_EXPLANATIONS_TO_MODEL} option us turned
 * on
 * <p>
 * The class is a simple placeholder for extra information. As you can see follows a static method
 * model so it is easy to use it from everywhere in the code
 * <p>
 * It simply hooks in to {@link CodegenModel} instances - using
 * {@link CodegenModel#getVendorExtensions()} part
 *
 * @author attil
 *
 */
public class ModelExplanations {

	public final static String X_MODEL_EXPLANATIONS = "x-keytiles-model-explanations";

	/**
	 * This method returns the {@link ModelExplanations} instance from the given model - if already
	 * exists. If not exists then will create and attach it
	 */
	public static ModelExplanations getOrCreateExplanations(CodegenModel model) {
		ModelExplanations explanations = (ModelExplanations) model.vendorExtensions.get(X_MODEL_EXPLANATIONS);
		if (explanations == null) {
			explanations = new ModelExplanations();
			model.vendorExtensions.put(X_MODEL_EXPLANATIONS, explanations);
		}
		return explanations;
	}

	/**
	 * This method returns the {@link ModelExplanations} instance from the given model - if exists.
	 * Otherwise it returns NULL
	 */
	public static ModelExplanations getExplanations(CodegenModel model) {
		return (ModelExplanations) model.vendorExtensions.get(X_MODEL_EXPLANATIONS);
	}

	/**
	 * You can append a new explanation message to the messages belong to the Class constructor.
	 * <p>
	 * note: this is just done if the model has an already registered {@link ModelExplanations} instance
	 * in it otherwise this method does nothing
	 *
	 * @param model
	 *            which model?
	 * @param message
	 *            your constructor explanation message
	 */
	public static void appendToConstructor(CodegenModel model, String message) {
		if (getExplanations(model) != null) {
			getExplanations(model).appendToConstructor(message);
		}
	}

	private final List<Map<String, String>> forConstructor = new LinkedList<>();

	public ModelExplanations() {
	}

	public List<Map<String, String>> getForConstructor() {
		return forConstructor;
	}

	private void appendToConstructor(String message) {
		Map<String, String> wrapper = new HashMap<>();
		wrapper.put("explanationMessage", message);

		forConstructor.add(wrapper);
	}

}
