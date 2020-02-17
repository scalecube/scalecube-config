package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.utils.ConfigCollectorUtil;
import io.scalecube.config.utils.ThrowableUtil;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class ClassPathConfigSource extends FilteredPathConfigSource {
  private static final String CLASSPATH = System.getProperty("java.class.path");
  private static final String PATH_SEPARATOR = System.getProperty("path.separator");

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

  static Collection<URI> getClassPathEntries(ClassLoader classloader) {
    Collection<URI> entries = new LinkedHashSet<>();
    ClassLoader parent = classloader.getParent();
    if (parent != null) {
      entries.addAll(getClassPathEntries(parent));
    }
    for (URL url : getClassLoaderUrls(classloader)) {
      if (url.getProtocol().equals("file")) {
        entries.add(toFile(url).toURI());
      }
    }
    return new LinkedHashSet<>(entries);
  }

  private static File toFile(URL url) {
    if (!url.getProtocol().equals("file")) {
      throw new IllegalArgumentException("Unsupported protocol in url: " + url);
    }
    try {
      return new File(url.toURI());
    } catch (URISyntaxException e) {
      return new File(url.getPath());
    }
  }

  private static Collection<URL> getClassLoaderUrls(ClassLoader classloader) {
    if (classloader instanceof URLClassLoader) {
      return Arrays.stream(((URLClassLoader) classloader).getURLs()).collect(Collectors.toSet());
    }
    if (classloader.equals(ClassLoader.getSystemClassLoader())) {
      return parseJavaClassPath();
    }
    return Collections.emptySet();
  }

  private static Collection<URL> parseJavaClassPath() {
    Collection<URL> urls = new LinkedHashSet<>();
    for (String entry : CLASSPATH.split(PATH_SEPARATOR)) {
      try {
        try {
          urls.add(new File(entry).toURI().toURL());
        } catch (SecurityException e) {
          urls.add(new URL("file", null, new File(entry).getAbsolutePath()));
        }
      } catch (MalformedURLException ex) {
        throw ThrowableUtil.propagate(ex);
      }
    }
    return new LinkedHashSet<>(urls);
  }

  private void scanDirectory(
      File directory, String prefix, Set<File> ancestors, Collection<Path> collector)
      throws IOException {
    File canonical = directory.getCanonicalFile();
    if (ancestors.contains(canonical)) {
      return;
    }
    File[] files = directory.listFiles();
    if (files == null) {
      return;
    }
    Set<File> objects = new LinkedHashSet<>(ancestors);
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
