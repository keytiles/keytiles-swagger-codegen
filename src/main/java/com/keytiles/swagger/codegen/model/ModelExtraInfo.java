package com.keytiles.swagger.codegen.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.keytiles.swagger.codegen.IKeytilesCodegen;
import com.keytiles.swagger.codegen.helper.debug.ModelExplanations;
import com.keytiles.swagger.codegen.helper.debug.PropertyExplanations;

import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenProperty;

/**
 * This helper class is calculating a few more things about the Models (will be later added to the
 * template variables) to support {@link ModelStyle#simpleConsistent} generation
 *
 * @author attil
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
	private List<CodegenProperty> publicFields = new ArrayList<>();
	// our local private final fields - will be taken from the Constructor
	private List<CodegenProperty> privateFinalFields = new ArrayList<>();
	// our local private fields
	private List<CodegenProperty> privateFields = new ArrayList<>();
	// these constructor arguments needed to pass into the super() part - belong to our parent model
	private List<CodegenProperty> ctorSuperArguments = new ArrayList<>();
	// these are fields who are non-nullable and they do not have a good defaultValue - so
	// needed to be taken from the constructor with non-null value
	private List<CodegenProperty> ctorNonNullablePrivateArguments = new ArrayList<>();
	// these are fields we have to validate on the constructor and do not accept NULL values
	private List<CodegenProperty> ctorValidateNonNullValueArguments = new ArrayList<>();

	private ModelExtraInfo(CodegenModel theModel, IKeytilesCodegen codeGenerator) {

		// let's find all local, mandatory (or read only) fields first
		for (CodegenProperty property : theModel.vars) {
			if (theModel.mandatory.contains(property.baseName) || property.getIsReadOnly()) {
				privateFinalFields.add(property);

				LOGGER.info("model {}, field '{}': becomes private final - as mandatory or readonly", theModel.name,
						property.baseName);
				PropertyExplanations.appendToProperty(property, "becomes private final - as mandatory or readonly");

			} else if (!property.nullable) {
				privateFields.add(property);

				LOGGER.info(
						"model {}, field '{}': becomes private - as non-nullable so we need to protect it with setter",
						theModel.name, property.baseName);
				PropertyExplanations.appendToProperty(property,
						"becomes private - as non-nullable so we need to protect it with setter");
				PropertyExplanations.appendToSetter(property,
						"added to protect field '" + property.baseName + "' against null-value assignment");

			} else {
				publicFields.add(property);

				LOGGER.info("model {}, field '{}': becomes public", theModel.name, property.baseName);
				PropertyExplanations.appendToProperty(property, "becomes public - as it has nothing special");
			}

			if (!property.nullable) {
				if (property.defaultValue == null || "null".equals(property.defaultValue)) {
					// OK we have a problem - we have a property who can not be null and we can not assign a good
					// default to it...
					// this property is already added to privateFields (because we need a setter) right?
					// but we need to enforce the user to give it a meaningful non-null value! so we need to
					// make it part of the constructor - if not already part of it...
					if (!privateFinalFields.contains(property)) {
						ctorNonNullablePrivateArguments.add(property);

						LOGGER.info(
								"model {}, field '{}': becomes constructor argument - as non-nullable and does not have a good defaultValue we can use on field level",
								theModel.name, property.baseName);
						PropertyExplanations.appendToProperty(property,
								"becomes private - becomes constructor argument - as non-nullable and does not have a good defaultValue we can use on field level");
					}
				}

				// ... so did it make into the constructor fields eventually?
				if (privateFinalFields.contains(property) || ctorNonNullablePrivateArguments.contains(property)) {
					// we will need to validate this guy for sure on the constructor
					ctorValidateNonNullValueArguments.add(property);
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
				ctorSuperArguments.addAll(parentInfo.ctorNonNullablePrivateArguments);
			}
		}

		// we will need a constructor if either our parent model requires us to do so OR we have mandatory
		// fields
		needsConstructor = !privateFinalFields.isEmpty() || !ctorSuperArguments.isEmpty()
				|| !ctorNonNullablePrivateArguments.isEmpty();

		if (needsConstructor) {
			ModelExplanations.appendToConstructor(theModel,
					"private final arguments: " + Joiner.on(", ").join(getArgAsString(privateFinalFields, null)));
			ModelExplanations.appendToConstructor(theModel, "non null arguments: "
					+ Joiner.on(", ").join(getArgAsString(ctorNonNullablePrivateArguments, null)));
		}

	}

	public boolean needsConstructor() {
		return needsConstructor;
	}

	public List<CodegenProperty> getPublicFields() {
		return publicFields;
	}

	public List<CodegenProperty> getPrivateFinalFields() {
		return privateFinalFields;
	}

	public List<CodegenProperty> getPrivateFields() {
		return privateFields;
	}

	public List<CodegenProperty> getCtorSuperArguments() {
		return ctorSuperArguments;
	}

	public List<CodegenProperty> getCtorNonNullablePrivateArguments() {
		return ctorNonNullablePrivateArguments;
	}

	public List<CodegenProperty> getCtorValidateNonNullValueArguments() {
		return ctorValidateNonNullValueArguments;
	}

	public List<CodegenProperty> getAllConstructorArgs() {
		List<CodegenProperty> args = new LinkedList<>();
		if (needsConstructor) {
			args.addAll(ctorSuperArguments);
			args.addAll(privateFinalFields);
			args.addAll(ctorNonNullablePrivateArguments);
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
				.join(getArgAsString(ctorNonNullablePrivateArguments, paramAnnotationTemplate));

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

	private List<String> getArgAsString(List<CodegenProperty> props, String paramAnnotationTemplate) {
		List<String> parts = new ArrayList<>(props.size());
		props.forEach(prop -> {
			String annotation = StringUtils.isBlank(paramAnnotationTemplate) ? ""
					: paramAnnotationTemplate.replace("{argName}", prop.baseName) + " ";
			parts.add(annotation + prop.datatypeWithEnum + " " + prop.name);
		});
		return parts;
	}

}
