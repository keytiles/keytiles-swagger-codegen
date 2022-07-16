package com.keytiles.swagger.codegen.helper.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

/**
 * Helper class for config options. This one is responsible reading up and storing an OpenAPI schema
 *
 * @see #fromFlatStringDefinition(String, String)
 *
 * @author attil
 *
 */
public class SchemaParam {

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaParam.class);

	public final static String OPT_MODEL_PACKAGE = "modelPackage";

	/**
	 * Creates an instance from a flat string definition which looks like this:
	 *
	 * <pre>
	 * "&lt;schemaFilePath&gt;[::modelPackage=&lt;package name&gt;]"
	 * </pre>
	 *
	 * @param configOptionName
	 *            the name of the config option this schema parameter is part of - this is for correct
	 *            error/warning displays user understands
	 */
	public static SchemaParam fromFlatStringDefinition(String configOptionName, String def) {
		List<String> parts = Splitter.on("::").limit(2).trimResults().splitToList(def);

		Preconditions.checkArgument(parts.size() > 0, "schemaFilePath is missing from definition");
		String schemaFilePath = parts.get(0);
		// needed - as openapi parser does not open the file otherwise
		schemaFilePath = schemaFilePath.replaceAll("\\\\", "/");

		String modelPackageOption = null;

		for (int idx = 1; idx < parts.size(); idx++) {
			List<String> optParts = Splitter.on('=').limit(2).trimResults().splitToList(parts.get(idx));
			String optName = optParts.get(0);
			if (OPT_MODEL_PACKAGE.equals(optName)) {
				Preconditions.checkArgument(optParts.size() == 2, "'%s' can not take empty package name",
						OPT_MODEL_PACKAGE);
				modelPackageOption = optParts.get(1);
			}
		}

		LOGGER.info("for config option '{}' {} is reading OppenApi schema file: {} ...", configOptionName,
				SchemaParam.class.getSimpleName(), schemaFilePath);

		OpenAPI openApiSchema = readOpenApiSchema(schemaFilePath);
		SchemaParam instance = new SchemaParam(schemaFilePath, configOptionName, modelPackageOption, openApiSchema);
		return instance;
	}

	private static OpenAPI readOpenApiSchema(String filePath) {
		List<AuthorizationValue> authorizationValues = null;

		/**
		 * for options docs see: https://github.com/swagger-api/swagger-parser#options
		 *
		 * The below is taken because this is the default settings swagger-codegen using
		 */

		boolean resolveFully = false;
		boolean flattenInlineSchema = false;

		ParseOptions options = new ParseOptions();
		options.setResolve(true);
		options.setResolveFully(resolveFully);
		options.setFlatten(true);
		options.setFlattenComposedSchemas(flattenInlineSchema);
		options.setSkipMatches(false);

		SwaggerParseResult result = new OpenAPIParser().readLocation(filePath, authorizationValues, options);
		OpenAPI openAPI = result.getOpenAPI();
		Preconditions.checkState(openAPI != null, "Oops! It looks swagger parser failed to read schema... messages: %s",
				result.getMessages());

		return openAPI;
	}

	private final String configOptionName;
	private final String schemaFilePath;
	private final String modelPackage;
	private final OpenAPI openAPI;

	private SchemaParam(String schemaFilePath, String configOptionName, String modelPackage, OpenAPI openAPI) {
		super();
		this.schemaFilePath = schemaFilePath;
		this.configOptionName = configOptionName;
		this.modelPackage = modelPackage;
		this.openAPI = openAPI;
	}

	public String getSchemaFilePath() {
		return schemaFilePath;
	}

	public OpenAPI getOpenAPI() {
		return openAPI;
	}

	public String getModelPackage() {
		return modelPackage;
	}

	public String getConfigOptionName() {
		return configOptionName;
	}

}
