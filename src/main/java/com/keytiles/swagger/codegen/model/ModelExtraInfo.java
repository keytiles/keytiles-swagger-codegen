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
import com.keytiles.swagger.codegen.IKeytilesCodegen.ModelState;
import com.keytiles.swagger.codegen.error.SchemaValidationException;
import com.keytiles.swagger.codegen.helper.CodegenUtil;
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

	/**
	 * You can use this static method to query the extra info associated with a model.
	 * <p>
	 * note: if the model has .parentModels then this query will recursively done upwards so by the time
	 * you get mack the extraInfo it is guaranteed that all .parentModels extraInfo is also generated
	 *
	 * @param theModel
	 *            which model you are interested in?
	 * @param codeGenerator
	 */
	public static ModelExtraInfo getExtraInfo(CodegenModel theModel, IKeytilesCodegen codeGenerator) {
		String fqClassName = codeGenerator.getModelFullyQualifiedName(theModel.name);
		ModelExtraInfo instance = instances.get(fqClassName);
		if (instance == null) {
			instance = new ModelExtraInfo(theModel, codeGenerator);
			instances.put(fqClassName, instance);
		}
		return instance;
	}

	/**
	 * For unit testing purposes - it is needed to be able to clean this "cache"
	 */
	public static void cleanStaticExtraInfoCache() {
		instances = new HashMap<>();
	}

	private final CodegenModel model;

	// do we need a constructor?
	private boolean needsConstructor = false;
	// our local public fields
	private Set<CodegenProperty> publicFields = new LinkedHashSet<>();
	// our local private final fields - will be taken from the Constructor
	private Set<CodegenProperty> privateFinalFields = new LinkedHashSet<>();
	// our local private fields
	private Set<CodegenProperty> privateFields = new LinkedHashSet<>();
	// these constructor arguments needed to to collect in order to pass into the super() part - belong
	// to our parent model
	private Set<CodegenProperty> ctorForSuperArguments = new LinkedHashSet<>();
	// these constructor arguments needed to be pass into the super() part - belong to our parent model
	private Set<CodegenProperty> ctorPassToSuperArguments = new LinkedHashSet<>();
	// these are parameters taken by constructor which will be assigned to class fields locally (mostly
	// private fields but
	// also can be even public fields)
	private Set<CodegenProperty> ctorOtherOwnFieldArguments = new LinkedHashSet<>();
	// these are fields we have to validate on the constructor and do not accept NULL values - its a
	// mixture from previous lists
	private Set<CodegenProperty> ctorValidateNonNullValueArguments = new LinkedHashSet<>();

	@SuppressWarnings("unchecked")
	@VisibleForTesting
	protected ModelExtraInfo(CodegenModel theModel, IKeytilesCodegen codeGenerator) {

		// we can not work with model which is not fully ready yet
		CodegenUtil.validateModelState(theModel, ModelState.fullyEnriched);

		this.model = theModel;

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

				if (CodegenUtil.isPropertyMandatory(theModel, property)) {
					ctorOtherOwnFieldArguments.add(property);
					ctorValidateNonNullValueArguments.add(property);

					LOGGER.info("model {}, field '{}': becomes constructor argument - as mandatory", theModel.name,
							property.baseName);
					ModelInlineMessages.appendToConstructor(theModel, ModelMessageType.EXPLANATION,
							"arg '" + property.name + "': mandatory field");
				} else if (!CodegenUtil.hasPropertyDefaultValue(theModel, property)) {
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

				if (CodegenUtil.isPropertyMandatory(theModel, property)) {
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

			if ("ContainerResponseClass".equals(theModel.name)) {
				LOGGER.info("buu");
			}

			ModelExtraInfo parentInfo = getExtraInfo(theModel.parentModel, codeGenerator);
			if (parentInfo.needsConstructor) {
				// it looks we have work to do! our parent class has a constructor
				// and this means we need to inherit all parameters of his constructor into ours
				ctorForSuperArguments.addAll(parentInfo.getAllConstructorArgs());
				// as a start we will also mark all collected things to be passed in the super(...) call
				ctorPassToSuperArguments.addAll(ctorForSuperArguments);
			}

			// Map<String, CodegenModel> allModels = codeGenerator.getAllModels();

			// let's check now if we have "overrides" at property level!
			for (CodegenProperty property : theModel.vars) {
				if (property.getBooleanValue(IKeytilesCodegen.X_PROPERTY_CONFLICTING_AND_RENAMED)) {

					CodegenModel conflictingModel = (CodegenModel) property.vendorExtensions
							.get(IKeytilesCodegen.X_PROPERTY_CONFLICTING_MODEL);

					// get the property from the superclass this property is overriding
					CodegenProperty superProperty = CodegenUtil.getPropertyByBaseName(conflictingModel,
							property.baseName);
					// are they compatible for value assignment?
					boolean isSuperAssignable = property
							.getBooleanValue(IKeytilesCodegen.X_PROPERTY_SUPER_IS_ASSIGNABLE);

					LOGGER.info(
							"model '{} extends {}' and property '{}' overlaps with property from this superclass. Is value in superclass compatible with this one: {}",
							theModel.name, conflictingModel.name, property.baseName, isSuperAssignable);

					if (isSuperAssignable) {
						// this value is compatible with the super field
						// and this means that if the super field was part of the super constructor, we can drop that
						// from collection and simply use ours
						CodegenProperty ctorReplaceCandidate = CodegenUtil
								.removePropertyByBaseName(ctorForSuperArguments, property.baseName);
						if (ctorReplaceCandidate != null) {
							// we removed it - so it was present!
							// this means we need to replace this with our own prop here
							// BUT(!)
							// in this case this argument can not be a List<> or Map<> or any type works with generic...
							if (ctorReplaceCandidate.getIsListContainer() || ctorReplaceCandidate.getIsMapContainer()
									|| ctorReplaceCandidate.getIsArrayModel()) {
								throw new SchemaValidationException("model '" + theModel.name + " extends "
										+ conflictingModel.name + "' and property '" + property.baseName
										+ "' overlaps with property from this superclass. This property is part of Constructor and using datatype "
										+ property.datatype
										+ " which is a type works with generic.\nIt is causing problems in Java, no way to put together a consistent class/constructor for this setup :-( You should avoid this property going into the Constructor! Make sure property is 'required=false' and 'readonly=false'! Also check section 'Java limitations with generating models' in README!");
							}

							ctorPassToSuperArguments = CodegenUtil.replacePropertyByBaseName(ctorPassToSuperArguments,
									property.baseName, property);
							LOGGER.info(
									"  - {}.{} property will be passed to superclass constructor arg for {}.{} as they are compatible",
									theModel.name, property.baseName, conflictingModel.name,
									ctorReplaceCandidate.baseName);

							// add an explanation to the constructor
							ModelInlineMessages.appendToConstructor(theModel, ModelMessageType.EXPLANATION,
									"arg '" + property.name
											+ "' is passed to super() call for superclass ctor argument '"
											+ ctorReplaceCandidate.name
											+ "' as value is compatible and schema name is the same");
						}
					} else {

						// we have incompatible types in Superclass and Subclass
						// Constructor of Superclass should be checked because that would cause issues!

						CodegenProperty ctorSuperArgumentWithSameSchemaName = CodegenUtil
								.getPropertyByBaseName(ctorForSuperArguments, property.baseName);
						if (ctorSuperArgumentWithSameSchemaName != null) {
							throw new SchemaValidationException("model '" + theModel.name + " extends "
									+ conflictingModel.name + "' and property '" + property.baseName
									+ "' overlaps with property from this Superclass.\nThe problem is that this property is taken by the Constructor of this Superclass. And data type in Superclass ("
									+ ctorSuperArgumentWithSameSchemaName.datatype
									+ ") is not compatible with data type (" + property.datatype
									+ ") in the Subclass... This would lead to generating a confusing model therefore we block it!\nYou should avoid this property going into the Constructor! Make sure property is 'required=false' and 'readonly=false'! Also check section 'Java limitations with generating models' in README!");
						}

					}

				}
			}

		}

		// we will need a constructor if either our parent model requires us to do so OR we have mandatory
		// fields
		needsConstructor = !privateFinalFields.isEmpty() || !ctorForSuperArguments.isEmpty()
				|| !ctorOtherOwnFieldArguments.isEmpty();

	}

	/**
	 * Checks the property attributes and might throw exception if hard error is detected
	 *
	 * @throws SchemaValidationException
	 */
	private void validatePropertyAttributes(CodegenModel model, CodegenProperty property) {
		if (CodegenUtil.hasPropertyUserAssignedDefaultValue(model, property)
				&& CodegenUtil.isPropertyMandatory(model, property)) {
			throw new SchemaValidationException("Invalid setup for " + model.name + "." + property.baseName
					+ ": a property can not be 'required=true' while having 'default: <value>' at the same time! It is contradicting as in OpenApi only optional properties should have default value - see https://swagger.io/docs/specification/describing-parameters, \"Default Parameter Values\" section!");
		}
	}

	public CodegenModel getModel() {
		return model;
	}

	public boolean needsConstructor() {
		return needsConstructor;
	}

	public Set<CodegenProperty> getPublicFields() {
		// we return a defensive copy
		return new LinkedHashSet<>(publicFields);
	}

	public Set<CodegenProperty> getPrivateFinalFields() {
		// we return a defensive copy
		return new LinkedHashSet<>(privateFinalFields);
	}

	public Set<CodegenProperty> getPrivateFields() {
		// we return a defensive copy
		return new LinkedHashSet<>(privateFields);
	}

	public Set<CodegenProperty> getCtorForSuperArguments() {
		// we return a defensive copy
		return new LinkedHashSet<>(ctorForSuperArguments);
	}

	public Set<CodegenProperty> getCtorPassToSuperArguments() {
		// we return a defensive copy
		return new LinkedHashSet<>(ctorPassToSuperArguments);
	}

	public Set<CodegenProperty> getCtorOwnFieldArguments() {
		// we return a defensive copy
		return new LinkedHashSet<>(ctorOtherOwnFieldArguments);
	}

	public Set<CodegenProperty> getCtorValidateNonNullValueArguments() {
		// we return a defensive copy
		return new LinkedHashSet<>(ctorValidateNonNullValueArguments);
	}

	public List<CodegenProperty> getAllConstructorArgs() {
		List<CodegenProperty> args = new LinkedList<>();
		if (needsConstructor) {
			args.addAll(ctorForSuperArguments);
			args.addAll(privateFinalFields);
			args.addAll(ctorOtherOwnFieldArguments);
		}
		return args;
	}

	/**
	 * @param propertyName
	 *            The name of the property (this is the name used in the model, not in the schema)
	 * @return the visibility like "public" or "private final" etc
	 */
	public String getVisibilityOfPropertyWithName(String propertyName) {
		for (CodegenProperty property : privateFinalFields) {
			if (propertyName.equals(property.name)) {
				return "private final";
			}
		}
		for (CodegenProperty property : privateFields) {
			if (propertyName.equals(property.name)) {
				return "private";
			}
		}
		for (CodegenProperty property : publicFields) {
			if (propertyName.equals(property.name)) {
				return "public";
			}
		}
		throw new IllegalStateException("It looks there is no property with name (in model) '" + propertyName
				+ "' in model '" + model.name + "'");
	}

	/**
	 * @param baseName
	 *            The name of the property in the schema, so baseName
	 * @return the visibility like "public" or "private final" etc
	 */
	public String getVisibilityOfPropertyWithBaseName(String baseName) {
		for (CodegenProperty property : privateFinalFields) {
			if (baseName.equals(property.baseName)) {
				return "private final";
			}
		}
		for (CodegenProperty property : privateFields) {
			if (baseName.equals(property.baseName)) {
				return "private";
			}
		}
		for (CodegenProperty property : publicFields) {
			if (baseName.equals(property.baseName)) {
				return "public";
			}
		}
		throw new IllegalStateException("It looks there is no property with baseName (name in schema) '" + baseName
				+ "' in model '" + model.name + "'");
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

		String superArgsString = Joiner.on(", ").join(getArgAsString(ctorForSuperArguments, paramAnnotationTemplate));
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
