package com.keytiles.swagger.codegen;

import org.junit.After;

import com.keytiles.swagger.codegen.testing.GeneratorForTests;

public class PlaygroundTest {

	private GeneratorForTests generator = null;

	@After
	public void cleanupAfterTestCase() {
		if (generator != null) {
			generator.deleteAllGeneratedFiles();
			generator.deleteOutputFolder();
			generator = null;
		}
	}

}
