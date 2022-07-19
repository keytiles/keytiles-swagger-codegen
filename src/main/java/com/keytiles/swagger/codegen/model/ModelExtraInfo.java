package com.keytiles.swagger.codegen.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.error.SchemaValidationException;
import com.keytiles.swagger.codegen.helper.debug.ModelInlineMessages;
import com.keytiles.swagger.codegen.helper.debug.ModelMessageType;
import com.keytiles.swagger.codegen.helper.debug.PropertyInlineMessages;

import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;

/**
 * This helper class is calculating a few more things about the Models (will be later added to the
 * template variables) to support {@link ModelStyle#simpleConsistent} generation
 *
 * @author attilaw
 *
 */
public class ModelExtraInfo {

	private final static Logger LOGGER = LoggerFactory.getLogger(ModelExtraInfo.class);

	private static Map<String, ModelExtraInfo> instances = new HashMap<>();

	public static ModelExtraInfo getExtraInfo(CodegenModel theModel, IKeytilesCodegen codeGenerator) {
		String fqClassName = codeGenerator.getModelFullyQualifiedName(theModel.name);
		ModelExtraInfo instance = instances.get(fqClassName);
		if (instance == null) {
			instance = new ModelExtraInfo(theModel, codeGenerator);
			instances.put(fqClassName, instance);
		}
		return instance;
	}

	// do we need a constructor?
	private boolean needsConstructor = false;
	// our local public fields
	private Set<CodegenProperty> publicFields = new LinkedHashSet<>();
	// our local private final fields - will be taken from the Constructor
	private Set<CodegenProperty> privateFinalFields = new LinkedHashSet<>();
	// our local private fields
	private Set<CodegenProperty> privateFields = new LinkedHashSet<>();
	// these constructor arguments needed to pass into the super() part - belong to our parent model
	private Set<CodegenProperty> ctorSuperArguments = new LinkedHashSet<>();
	// these are parameters taken by constructor which will be assigned to class fields locally (mostly
	// private fields but
	// also can be even public fields)
	private Set<CodegenProperty> ctorOtherOwnFieldArguments = new LinkedHashSet<>();
	// these are fields we have to validate on the constructor and do not accept NULL values - its a
	// mixture from previous lists
	private Set<CodegenProperty> ctorValidateNonNullValueArguments = new LinkedHashSet<>();

	@VisibleForTesting
	protected ModelExtraInfo(CodegenModel theModel, IKeytilesCodegen codeGenerator) {

		// let's find all local, mandatory (or read only) fields first
		for (CodegenProperty property : theModel.vars) {

			// some hard schema validations
			validatePropertyAttributes(theModel, property);

			if (property.getIsReadOnly()) {
				privateFinalFields.add(property);

				LOGGER.info("model {}, field '{}': becomes private final - as mandatory AND readonly", theModel.name,
						property.baseName);
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"becomes private final - as readonly");

				if (!property.nullable) {
					ctorValidateNonNullValueArguments.add(property);

					ModelInlineMessages.appendToConstructor(theModel, ModelMessageType.EXPLANATION, "arg '"
							+ property.name
							+ "': private final field because it is readonly (also non-null check as not nullable)");
				} else {
					ModelInlineMessages.appendToConstructor(theModel, ModelMessageType.EXPLANATION,
							"arg '" + property.name + "': private final field because it is readonly");
				}

			} else if (!property.nullable) {
				privateFields.add(property);

				LOGGER.info(
						"model {}, field '{}': becomes private - as non-nullable so we need to protect it with setter and null-check",
						theModel.name, property.baseName);
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"becomes private - as non-nullable so we need to protect it with setter and null-check");
				PropertyInlineMessages.appendToSetter(property, ModelMessageType.EXPLANATION,
						"added to protect field '" + property.baseName + "' against null-value assignment");

				if (isPropertyMandatory(theModel, property)) {
					ctorOtherOwnFieldArguments.add(property);
					ctorValidateNonNullValueArguments.add(property);

					LOGGER.info("model {}, field '{}': becomes constructor argument - as mandatory", theModel.name,
							property.baseName);
					ModelInlineMessages.appendToConstructor(theModel, ModelMessageType.EXPLANATION,
							"arg '" + property.name + "': mandatory field");
				} else if (!hasPropertyDefaultValue(theModel, property)) {
					ctorOtherOwnFieldArguments.add(property);
					ctorValidateNonNullValueArguments.add(property);

					LOGGER.info(
							"model {}, field '{}': becomes constructor argument - as non-nullable and does not have default value - we must enforce non-null initial value",
							theModel.name, property.baseName);
					ModelInlineMessages.appendToConstructor(theModel, ModelMessageType.EXPLANATION, "arg '"
							+ property.name
							+ "': non-nullable and does not have default value - we must enforce a non-null initial value");
				}
			} else {
				publicFields.add(property);

				LOGGER.info(
						"model {}, field '{}': becomes public - as nullable (no need to null-check) and not readonly",
						theModel.name, property.baseName);
				PropertyInlineMessages.appendToProperty(property, ModelMessageType.EXPLANATION,
						"becomes public - as nullable (no need to null-check) and not readonly");

				if (isPropertyMandatory(theModel, property)) {
					ctorOtherOwnFieldArguments.add(property);

					LOGGER.info("model {}, field '{}': becomes constructor argument - as mandatory", theModel.name,
							property.baseName);
					ModelInlineMessages.appendToConstructor(theModel, ModelMessageType.EXPLANATION,
							"arg '" + property.name + "': mandatory field (but NULL value is accepted too)");
				}
			}

		}

		// now it's time to focus on our parent - if we have any
		if (theModel.parentModel != null) {
			ModelExtraInfo parentInfo = getExtraInfo(theModel.parentModel, codeGenerator);
			if (parentInfo.needsConstructor) {
				// it looks we have work to do! our parent class has a constructor
				// and this means we need to inherit all parameters of his constructor into ours
				ctorSuperArguments.addAll(parentInfo.ctorSuperArguments);
				ctorSuperArguments.addAll(parentInfo.privateFinalFields);
				ctorSuperArguments.addAll(parentInfo.ctorOtherOwnFieldArguments);
			}
		}

		// we will need a constructor if either our parent model requires us to do so OR we have mandatory
		// fields
		needsConstructor = !privateFinalFields.isEmpty() || !ctorSuperArguments.isEmpty()
				|| !ctorOtherOwnFieldArguments.isEmpty();

	}

	/**
	 * Checks if a property is mandatory in the model
	 */
	private boolean isPropertyMandatory(CodegenModel model, CodegenProperty property) {
		return model.mandatory.contains(property.baseName);
	}

	/**
	 * Checks if the property has a default value or not
	 *
	 */
	private boolean hasPropertyDefaultValue(CodegenModel model, CodegenProperty property) {
		// return property.defaultValue != null && !"null".equals(property.defaultValue);
		return property.jsonSchema.contains("\"default\"");
	}

	/**
	 * Checks the property attributes and might throw exception if hard error is detected
	 *
	 * @throws SchemaValidationException
	 */
	private void validatePropertyAttributes(CodegenModel model, CodegenProperty property) {
		if (hasPropertyDefaultValue(model, property) && isPropertyMandatory(model, property)) {
			throw new SchemaValidationException("Invalid setup for " + model.name + "." + property.baseName
					+ ": a property can not be 'required=true' while having 'default: <value>' at the same time! It is contradicting as in OpenApi only optional properties should have default value - see https://swagger.io/docs/specification/describing-parameters, \"Default Parameter Values\" section!");
		}
	}

	public boolean needsConstructor() {
		return needsConstructor;
	}

	public Set<CodegenProperty> getPublicFields() {
		return publicFields;
	}

	public Set<CodegenProperty> getPrivateFinalFields() {
		return privateFinalFields;
	}

	public Set<CodegenProperty> getPrivateFields() {
		return privateFields;
	}

	public Set<CodegenProperty> getCtorSuperArguments() {
		return ctorSuperArguments;
	}

	public Set<CodegenProperty> getCtorOwnFieldArguments() {
		return ctorOtherOwnFieldArguments;
	}

	public Set<CodegenProperty> getCtorValidateNonNullValueArguments() {
		return ctorValidateNonNullValueArguments;
	}

	public List<CodegenProperty> getAllConstructorArgs() {
		List<CodegenProperty> args = new LinkedList<>();
		if (needsConstructor) {
			args.addAll(ctorSuperArguments);
			args.addAll(privateFinalFields);
			args.addAll(ctorOtherOwnFieldArguments);
		}
		return args;
	}

	/**
	 * Mustache template suxxx - to combine the constructor arguments together there with all correct
	 * "," between is mission impossible. So this method can be invoked to combine everything together
	 * in java and just put it to the template
	 *
	 * @param paramAnnotationTemplate
	 *            this will be prepanded as annotation to each param - use "{argName}" as marker which
	 *            will be replaced with name of property
	 * @return a joined string you just need to drop into the template
	 */
	public String getConstructorCombinedArgsAsString(String paramAnnotationTemplate) {
		if (!needsConstructor) {
			return "";
		}

		String superArgsString = Joiner.on(", ").join(getArgAsString(ctorSuperArguments, paramAnnotationTemplate));
		String privateFinalArgsString = Joiner.on(", ")
				.join(getArgAsString(privateFinalFields, paramAnnotationTemplate));
		String privateNonNullableArgsString = Joiner.on(", ")
				.join(getArgAsString(ctorOtherOwnFieldArguments, paramAnnotationTemplate));

		List<String> parts = new ArrayList<>();
		if (StringUtils.isNoneBlank(superArgsString)) {
			parts.add(superArgsString);
		}
		if (StringUtils.isNoneBlank(privateFinalArgsString)) {
			parts.add(privateFinalArgsString);
		}
		if (StringUtils.isNoneBlank(privateNonNullableArgsString)) {
			parts.add(privateNonNullableArgsString);
		}
		return Joiner.on(", ").join(parts);
	}

	private List<String> getArgAsString(Collection<CodegenProperty> props, String paramAnnotationTemplate) {
		List<String> parts = new ArrayList<>(props.size());
		props.forEach(prop -> {
			String annotation = StringUtils.isBlank(paramAnnotationTemplate) ? ""
					: paramAnnotationTemplate.replace("{argName}", prop.baseName) + " ";
			parts.add(annotation + prop.datatypeWithEnum + " " + prop.name);
		});
		return parts;
	}

}
