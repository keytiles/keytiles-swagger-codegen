package com.keytiles.swagger.codegen.testing;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.keytiles.swagger.codegen.IKeytilesCodegen;

import io.swagger.codegen.v3.DefaultGenerator;

public class GeneratorForTests extends DefaultGenerator {

	private final Class<?> testClass;
	private List<File> files;

	public GeneratorForTests(Class<?> testClass) {
		super();
		this.testClass = testClass;
	}

	@Override
	public List<File> generate() {
		files = super.generate();
		return files;
	}

	public void deleteAllGeneratedFiles() {
		if (files == null) {
			return;
		}
		for (File file : files) {
			file.delete();
		}
	}

	public void deleteOutputFolder() {
		try {
			FileUtils.deleteDirectory(new File(opts.getConfig().outputFolder()));
		} catch (IOException e) {
			// what now?
			System.err.println("failed to delete temp folder of test " + testClass.getSimpleName()
					+ " used due to exception: " + e);
		}

	}

	public IKeytilesCodegen getCodegen() {
		return (IKeytilesCodegen) this.opts.getConfig();
	}

}
