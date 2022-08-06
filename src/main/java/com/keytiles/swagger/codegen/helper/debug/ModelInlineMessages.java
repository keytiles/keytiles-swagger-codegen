package com.keytiles.swagger.codegen.helper.debug;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.model.ModelExtraInfo;

import io.swagger.codegen.v3.CodegenModel;

/**
 * This class is used when {@link IKeytilesCodegen#OPT_ADD_EXPLANATIONS_TO_MODEL} option is turned
 * on OR {@link ModelExtraInfo} add warnings.
 * <p>
 * Messages are categorized and will be rendered to the class. As you can see follows a static
 * method model so it is easy to use it from everywhere in the code
 * <p>
 * It simply hooks in to {@link CodegenModel} instances - using
 * {@link CodegenModel#getVendorExtensions()} part
 *
 * @author attilaw
 *
 */
public class ModelInlineMessages {

	public final static String X_MODEL_EXPLANATIONS = IKeytilesCodegen.COMPUTED_VENDOR_PREFIX + "model-explanations";

	/**
	 * This method returns the {@link ModelInlineMessages} instance from the given model - if already
	 * exists. If not exists then will create and attach it
	 */
	public static ModelInlineMessages getOrCreateMessages(CodegenModel model, ModelMessageType type) {
		ModelInlineMessages explanations = (ModelInlineMessages) model.vendorExtensions.get(X_MODEL_EXPLANATIONS);
		if (explanations == null) {
			explanations = new ModelInlineMessages();
			model.vendorExtensions.put(X_MODEL_EXPLANATIONS, explanations);
		}
		return explanations;
	}

	/**
	 * This method returns the {@link ModelInlineMessages} instance from the given model - if exists.
	 * Otherwise it returns NULL
	 */
	public static ModelInlineMessages getMessages(CodegenModel model, ModelMessageType type) {
		return (ModelInlineMessages) model.vendorExtensions.get(X_MODEL_EXPLANATIONS);
	}

	private static String prefixMessage(ModelMessageType type, String message) {
		if (type == ModelMessageType.WARNING) {
			return "WARNING - " + message;
		}
		return message;
	}

	/**
	 * You can append a new explanation message to the messages belong to the Class constructor.
	 * <p>
	 * note: this is just done if the model has an already registered {@link ModelInlineMessages}
	 * instance in it otherwise this method does nothing
	 *
	 * @param model
	 *            which model?
	 * @param type
	 *            what type of message is this?
	 * @param message
	 *            your constructor explanation message
	 */
	public static void appendToConstructor(CodegenModel model, ModelMessageType type, String message) {
		ModelInlineMessages messages = type == ModelMessageType.WARNING ? getOrCreateMessages(model, type)
				: getMessages(model, type);

		if (messages != null) {
			messages.appendToConstructor(prefixMessage(type, message));
		}
	}

	private final List<Map<String, String>> forConstructor = new LinkedList<>();

	public ModelInlineMessages() {
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
