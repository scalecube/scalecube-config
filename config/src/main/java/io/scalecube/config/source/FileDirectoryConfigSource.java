package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import java.io.File;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FileDirectoryConfigSource extends FilteredPathConfigSource {
  private final Path directory;

  /**
   * Constructor.
   *
   * @param directory directory with configuration files
   * @param predicate predicate to match confgitratyion files
   */
  public FileDirectoryConfigSource(String directory, Predicate<Path> predicate) {
    this(directory, Collections.singletonList(predicate));
  }

  /**
   * Constructor.
   *
   * @param directory directory with configuration files
   * @param predicates list of predicates to match configuration files
   */
  public FileDirectoryConfigSource(String directory, List<Predicate<Path>> predicates) {
    super(predicates);
    Objects.requireNonNull(directory, "FileDirectoryConfigSource: directory is required");
    this.directory = Paths.get(directory);
  }

  /**
   * Factory method to create {@code FileDirectoryConfigSource} instance by configuration file mask
   * plus its prefixes.
   *
   * @param directory directory with configuration files
   * @param mask mask for template of configuration property file
   * @param prefixes list of prefixes (comma separated list of strings)
   * @return new {@code FileDirectoryConfigSource} instance
   */
  public static FileDirectoryConfigSource createWithPattern(
      String directory, String mask, List<String> prefixes) {
    Objects.requireNonNull(directory, "FileDirectoryConfigSource: directory is required");
    Objects.requireNonNull(mask, "FileDirectoryConfigSource: mask is required");
    Objects.requireNonNull(mask, "FileDirectoryConfigSource: prefixes is required");

    Pattern pattern = Pattern.compile(mask);

    Predicate<Path> patternPredicate =
        path -> pattern.matcher(path.getFileName().toString()).matches();

    List<Predicate<Path>> predicates =
        prefixes.stream()
            .map(p -> (Predicate<Path>) path -> path.getFileName().startsWith(p))
            .map(patternPredicate::and)
            .collect(Collectors.toList());

    return new FileDirectoryConfigSource(directory, predicates);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    Path realDirectory;
    try {
      realDirectory = directory.toRealPath(LinkOption.NOFOLLOW_LINKS);
    } catch (Exception e) {
      String message =
          String.format(
              "Exception at FileDirectoryConfigSource (directory='%s'), cause: %s", directory, e);
      throw new ConfigSourceNotAvailableException(message, e);
    }

    File[] files = Optional.ofNullable(realDirectory.toFile().listFiles()).orElse(new File[0]);
    List<Path> pathCollection = Arrays.stream(files).map(File::toPath).collect(Collectors.toList());

    Map<String, ConfigProperty> result = new TreeMap<>();

    filterAndCollectInOrder(
        predicates.iterator(),
        loadConfigMap(pathCollection),
        (path, map) ->
            map.entrySet()
                .forEach(
                    entry ->
                        result.putIfAbsent(
                            entry.getKey(),
                            LoadedConfigProperty.withNameAndValue(entry)
                                .origin(path.toString())
                                .build())));

    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", FileDirectoryConfigSource.class.getSimpleName() + "[", "]")
        .add("directory=" + directory)
        .toString();
  }
}
