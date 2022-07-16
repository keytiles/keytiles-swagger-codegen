package com.keytiles.swagger.codegen.helper.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

/**
 * Helper class for config options. This one is reading up a list of {@link SchemaParam} definitions
 *
 * @see #fromFlatStringDefinition(String, String)
 *
 * @author attil
 *
 */
public class SchemaParamCollection {

	public static SchemaParamCollection fromFlatStringDefinition(String configOptionName, String def) {
		SchemaParamCollection collection = new SchemaParamCollection(configOptionName);

		List<String> schemaParamDefFlatStrings = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(def);
		schemaParamDefFlatStrings.forEach(schemaParamDefFlatStr -> {
			SchemaParam schemaParam = SchemaParam.fromFlatStringDefinition(configOptionName, schemaParamDefFlatStr);
			collection.addSchemaParam(schemaParam);
		});

		return collection;
	}

	private final String configOptionName;
	private final Map<String, SchemaParam> schemaParams;

	private SchemaParamCollection(String configOptionName) {
		// we need to maintain order here
		schemaParams = new LinkedHashMap<>();
		this.configOptionName = configOptionName;
	}

	public String getConfigOptionName() {
		return configOptionName;
	}

	private void addSchemaParam(SchemaParam schemaParam) {
		Preconditions.checkArgument(!schemaParams.containsKey(schemaParam.getSchemaFilePath()),
				"Oops! Duplicated schema addition for config option '%s'! Schema '%s' is already added...",
				configOptionName, schemaParam.getConfigOptionName());

		schemaParams.put(schemaParam.getSchemaFilePath(), schemaParam);
	}

	/**
	 * @return the added schema params - in the order they were added with
	 *         {@link #addSchemaParam(SchemaParam)}
	 */
	public Map<String, SchemaParam> getSchemaParamsInAdditionOrder() {
		// we return a deffensive copy
		return new HashMap<>(schemaParams);
	}

}
