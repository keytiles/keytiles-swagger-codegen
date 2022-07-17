package com.keytiles.swagger.codegen.helper;

import java.util.Collection;

import com.google.common.base.Joiner;

import io.swagger.codegen.v3.CodegenConfig;
import io.swagger.codegen.v3.DefaultGenerator;

/**
 * Static util class to pass things to the {@link DefaultGenerator} object which is used hardcoded
 * way from e.g. maven plugin and CLI
 * <p>
 * Unfortunately it looks we can just write our own {@link CodegenConfig}s - which then later
 * instantiated through the "language" oprion.
 * <p>
 * But luckily the {@link CodegenConfig} (so our code) runs as first step in
 * {@link DefaultGenerator#generate()} method AND the {@link DefaultGenerator} is reading things
 * from {@link System#getProperties()} and this gives us some chance to customize this/that even for
 * the generator itself :-)
 *
 * @author attilaw
 *
 */
public class GeneratorInjectionHelper {

	public final static String GENERATOR_SYSPROP_GENERATE_MODELS = "models";

	private GeneratorInjectionHelper() {
	}

	/**
	 * Passing the model names to generate to the generator ('generateModels' option) via
	 * {@link #GENERATOR_SYSPROP_GENERATE_MODELS} system property as comma separated list
	 *
	 * @param modelNames
	 *            the name of the models to generate - other models will be skipped during generation
	 */
	public static void injectModelsToGenerate(Collection<String> modelNames) {
		System.getProperties().put(GENERATOR_SYSPROP_GENERATE_MODELS, Joiner.on(',').join(modelNames));
	}

}
