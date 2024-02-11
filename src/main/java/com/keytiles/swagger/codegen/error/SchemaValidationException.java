package com.keytiles.swagger.codegen.error;

/**
 * Thrown if generator runs into issue regarding the schema
 *
 * @author attilaw
 *
 */
public class SchemaValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SchemaValidationException(String message) {
		super(message);
	}

	public SchemaValidationException(String message, Throwable cause) {
		super(message, cause);
	}

}
