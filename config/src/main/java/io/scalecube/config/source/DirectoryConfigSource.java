package io.scalecube.config.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.utils.ConfigCollectorUtil;
import io.scalecube.config.utils.ThrowableUtil;

public class DirectoryConfigSource extends FilteredPathConfigSource {
	private final Path basePath;

	public DirectoryConfigSource(@Nonnull String basePath, @Nonnull List<Predicate<Path>> predicates) {
		super(predicates);
		this.basePath = Paths.get(Objects.requireNonNull(basePath, "DirectoryConfigSource: basePath is required"));
	}

	@SafeVarargs
	public DirectoryConfigSource(@Nonnull String basePath, @Nonnull Predicate<Path>... predicates) {
		this(basePath, Arrays.asList(predicates));
	}

	@Override
	public Map<String, ConfigProperty> loadConfig() {
		Path basePath1;
		try {
			basePath1 = basePath.toRealPath(LinkOption.NOFOLLOW_LINKS);
		} catch (IOException e) {
			throw ThrowableUtil.propagate(e);
		}

		File[] files = Optional.ofNullable(basePath1.toFile().listFiles()).orElse(new File[0]);
		List<Path> pathCollection = Arrays.stream(files).map(File::toPath).collect(Collectors.toList());

		Map<Path, Map<String, String>> configMap = loadConfigMap(pathCollection);

		Map<String, ConfigProperty> result = new TreeMap<>();
		ConfigCollectorUtil.filterAndCollectInOrder(predicates.iterator(), configMap,
				(path, map) -> map.entrySet().forEach(
						entry -> result.putIfAbsent(entry.getKey(),
								LoadedConfigProperty.withNameAndValue(entry).origin(path.toString()).build())));

		return result;
	}

	@Override
	public String toString() {
		return "DirectoryConfigSource{" +
				"basePath='" + basePath.toAbsolutePath() + "'" +
				'}';
	}
}
