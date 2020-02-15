package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ClassPathConfigSource extends FilteredPathConfigSource {
  private Map<String, ConfigProperty> loadedConfig;

  /**
   * Constructor.
   *
   * @param predicate predicate to match configuration files
   */
  public ClassPathConfigSource(Predicate<Path> predicate) {
    this(Collections.singletonList(predicate));
  }

  /**
   * Constructor.
   *
   * @param predicates list of predicates to match configuration files
   */
  public ClassPathConfigSource(List<Predicate<Path>> predicates) {
    super(predicates);
  }

  /**
   * Factory method to create {@code ClassPathConfigSource} instance by configuration file mask plus
   * its prefixes.
   *
   * @param mask mask for template of configuration property file
   * @param prefixes list of prefixes (comma separated list of strings)
   * @return new {@code ClassPathConfigSource} instance
   */
  public static ClassPathConfigSource createWithPattern(String mask, List<String> prefixes) {
    Objects.requireNonNull(mask, "ClassPathConfigSource: mask is required");
    Objects.requireNonNull(prefixes, "ClassPathConfigSource: prefixes is required");

    Pattern pattern = Pattern.compile(mask);

    Predicate<Path> patternPredicate =
        path -> pattern.matcher(path.getFileName().toString()).matches();

    List<Predicate<Path>> predicates =
        prefixes.stream()
            .map(p -> (Predicate<Path>) path -> path.getFileName().startsWith(p))
            .map(patternPredicate::and)
            .collect(Collectors.toList());

    return new ClassPathConfigSource(predicates);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    if (loadedConfig != null) {
      return loadedConfig;
    }

    Collection<Path> pathCollection = new ArrayList<>();
    getClassPathEntries(getClass().getClassLoader()).stream()
        .filter(uri -> uri.getScheme().equals("file"))
        .forEach(
            uri -> {
              File file = new File(uri);
              if (file.exists()) {
                try {
                  if (file.isDirectory()) {
                    scanDirectory(file, "", Collections.emptySet(), pathCollection);
                  } else {
                    scanJar(file, pathCollection);
                  }
                } catch (Exception e) {
                  throw ThrowableUtil.propagate(e);
                }
              }
            });

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

    return loadedConfig = result;
  }

  private Collection<URI> getClassPathEntries(ClassLoader classLoader) {
    Collection<URI> entries = new LinkedHashSet<>();
    // Search parent first, since it's the order ClassLoader#loadClass() uses.
    ClassLoader parent = classLoader.getParent();
    if (parent != null) {
      entries.addAll(getClassPathEntries(parent));
    }
    if (classLoader instanceof URLClassLoader) {
      URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
      for (URL entry : urlClassLoader.getURLs()) {
        try {
          entries.add(entry.toURI());
        } catch (URISyntaxException e) {
          throw ThrowableUtil.propagate(e);
        }
      }
    }
    return new LinkedHashSet<>(entries);
  }

  private void scanDirectory(
      File directory, String prefix, Set<File> ancestors, Collection<Path> collector)
      throws IOException {
    File canonical = directory.getCanonicalFile();
    if (ancestors.contains(canonical)) {
      // A cycle in the filesystem, for example due to a symbolic link.
      return;
    }
    File[] files = directory.listFiles();
    if (files == null) {
      return;
    }
    HashSet<File> objects = new HashSet<>();
    objects.addAll(ancestors);
    objects.add(canonical);
    Set<File> newAncestors = Collections.unmodifiableSet(objects);
    for (File f : files) {
      String name = f.getName();
      if (f.isDirectory()) {
        scanDirectory(f, prefix + name + "/", newAncestors, collector);
      } else {
        collector.add(f.toPath());
      }
    }
  }

  private void scanJar(File file, Collection<Path> collector) throws IOException {
    JarFile jarFile;
    try {
      jarFile = new JarFile(file);
    } catch (IOException ignore) {
      return;
    }
    try (FileSystem zipfs = FileSystems.newFileSystem(file.toPath(), null)) {
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        collector.add(zipfs.getPath(entry.getName()));
      }
    } finally {
      try {
        jarFile.close();
      } catch (IOException ignore) {
        // ignore
      }
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ClassPathConfigSource.class.getSimpleName() + "[", "]")
        .add("classLoader=" + getClass().getClassLoader())
        .toString();
  }
}
