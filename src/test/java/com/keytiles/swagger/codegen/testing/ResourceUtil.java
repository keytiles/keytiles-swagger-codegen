package com.keytiles.swagger.codegen.testing;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

import com.google.common.io.Resources;

/**
 * Collection of static helper methods for supporting resource handling like loading file content
 * etc.
 *
 * @author AttilaW
 *
 */
public class ResourceUtil {

	public static final String CLASSPATH_RESOURCE_PREFIX = "classpath:";

	private final static Pattern WINDOWS_ABSOLUTE_FILEPATH = Pattern.compile("^\\/[a-zA-Z]{1}:.*");

	private ResourceUtil() {
	}

	/**
	 * You give this a path on the classpath and this method returns a real filesystem path for you
	 * <p>
	 * IMPORTANT! The resource you point to with the pathOnClasspath must exist!
	 */
	public static String getRealFilesystemPathForResource(String pathOnClasspath) {
		URL url = Resources.getResource(pathOnClasspath);
		String path = url.getPath();
		if (WINDOWS_ABSOLUTE_FILEPATH.matcher(path).matches()) {
			path = path.substring(1);
		}
		return path;
	}

	public static String loadResourceTextFileContent(String pathOnClasspath) {
		URL url = Resources.getResource(pathOnClasspath);
		try {
			return Resources.toString(url, Charset.forName("utf-8"));
		} catch (IOException e) {
			throw new RuntimeException("failed to load text file content '" + pathOnClasspath + "'", e);
		}
	}

}
