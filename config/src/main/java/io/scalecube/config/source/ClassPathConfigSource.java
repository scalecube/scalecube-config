package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.utils.ConfigCollectorUtil;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ClassPathConfigSource extends FilteredPathConfigSource {
  private final ClassLoader classLoader;

  private Map<String, ConfigProperty> loadedConfig;

  /**
   * Creates provider of configuration properties with classpath as source.
   *
   * @param classLoader class loader
   * @param predicates list of predicates to filter
   */
  public ClassPathConfigSource(ClassLoader classLoader, List<Predicate<Path>> predicates) {
    super(predicates);
    this.classLoader =
        Objects.requireNonNull(classLoader, "ClassPathConfigSource: classloader is required");
  }

  public ClassPathConfigSource(List<Predicate<Path>> predicates) {
    this(ClassPathConfigSource.class.getClassLoader(), predicates);
  }

  @SafeVarargs
  public ClassPathConfigSource(ClassLoader classLoader, Predicate<Path>... predicates) {
    super(Arrays.asList(predicates));
    this.classLoader = classLoader;
  }

  @SafeVarargs
  public ClassPathConfigSource(Predicate<Path>... predicates) {
    this(ClassPathConfigSource.class.getClassLoader(), Arrays.asList(predicates));
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    if (loadedConfig != null) {
      return loadedConfig;
    }

    Collection<Path> pathCollection = new ArrayList<>();
    getClassPathEntries(classLoader).stream()
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

    Map<Path, Map<String, String>> configMap = loadConfigMap(pathCollection);

    Map<String, ConfigProperty> result = new TreeMap<>();
    ConfigCollectorUtil.filterAndCollectInOrder(
        predicates.iterator(),
        configMap,
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
    return "ClassPathConfigSource{" + "classLoader=" + classLoader + '}';
  }
}
