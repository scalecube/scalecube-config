package io.scalecube.config.examples;

import java.nio.file.Path;
import java.util.function.Predicate;

import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.source.ClassPathConfigSource;
import io.scalecube.config.source.DirectoryConfigSource;
import io.scalecube.config.StringConfigProperty;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class LocalResourceConfigExample {

	public static void main(String[] args) {
		Predicate<Path> propsPredicate = path -> path.toString().endsWith(".props");

		String basePath = "config-examples/config";

		ConfigRegistry configRegistry = ConfigRegistry.create(
				ConfigRegistrySettings.builder()
						.addLastSource("classpath", new ClassPathConfigSource(propsPredicate))
						.addLastSource("configDirectory", new DirectoryConfigSource(basePath, propsPredicate))
						.jmxEnabled(false)
						.build());

		StringConfigProperty prop1 = configRegistry.stringProperty("prop1");
		StringConfigProperty prop2 = configRegistry.stringProperty("prop2");

		System.out.println("### Classpath property: prop1=" + prop1.get().get());
		System.out.println("### Property existing only in filesystem: prop2=" + prop2.get().get());
	}
}
