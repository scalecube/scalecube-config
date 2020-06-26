package io.scalecube.config.source;

import io.scalecube.config.ConfigProperty;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public final class ClassPathConfigSource extends FilteredPathConfigSource {

  private static final String CLASSPATH = System.getProperty("java.class.path");
  private static final String PATH_SEPARATOR = System.getProperty("path.separator");
  private static final String CLASSPATH_ATTIBUTE_MANIFEST_SEPARATOR = " ";

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
   * Factory method to create {@code ClassPathConfigSource} instance using filename plus its
   * prefixPatterns.
   *
   * @param filename filename for template of configuration property file
   * @param prefixPattern pattern of prefix
   * @return new {@code ClassPathConfigSource} instance
   */
  public static ClassPathConfigSource createWithPattern(String filename, String prefixPattern) {
    return createWithPattern(filename, Collections.singletonList(prefixPattern));
  }

  /**
   * Factory method to create {@code ClassPathConfigSource} instance using filename plus its
   * prefixPatterns.
   *
   * @param filename filename for template of configuration property file
   * @param prefixPatterns list of prefixPatterns (comma separated list of strings)
   * @return new {@code ClassPathConfigSource} instance
   */
  public static ClassPathConfigSource createWithPattern(
      String filename, List<String> prefixPatterns) {
    Objects.requireNonNull(filename, "ClassPathConfigSource: filename is required");
    Objects.requireNonNull(prefixPatterns, "ClassPathConfigSource: prefixPatterns is required");
    return new ClassPathConfigSource(preparePatternPredicates(filename, prefixPatterns));
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
              if (!file.exists()) {
                return;
              }
              try {
                if (file.isDirectory()) {
                  Set<File> currentPath =
                      new HashSet<>(Collections.singleton(file.getCanonicalFile()));
                  scanDirectory(file, "", currentPath, pathCollection);
                } else {
                  scanJar(file, pathCollection);
                }
              } catch (Exception e) {
                throw ThrowableUtil.propagate(e);
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

  private static Collection<URI> getClassPathEntries(ClassLoader classloader) {
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
          throw ThrowableUtil.propagate(e);
        }
      } catch (MalformedURLException ex) {
        throw ThrowableUtil.propagate(ex);
      }
    }
    return new LinkedHashSet<>(urls);
  }

  private static void scanDirectory(
      File directory, String prefix, Set<File> currentPath, Collection<Path> collector)
      throws IOException {

    File[] files = directory.listFiles();
    if (files == null) {
      return;
    }

    for (File f : files) {
      String name = f.getName();
      if (f.isDirectory()) {
        File deref = f.getCanonicalFile();
        if (currentPath.add(deref)) {
          scanDirectory(deref, prefix + name + "/", currentPath, collector);
          currentPath.remove(deref);
        }
      } else {
        String resourceName = prefix + name;
        if (!resourceName.equals(JarFile.MANIFEST_NAME)) {
          collector.add(f.toPath());
        }
      }
    }
  }

  private static void scanJar(File file, Collection<Path> collector) throws IOException {
    try (JarFile jarFile = new JarFile(file)) {
      for (File path : getClassPathFromManifest(file, jarFile.getManifest())) {
        if (collector.add(path.getCanonicalFile().toPath())) {
          scanFrom(path, collector);
        }
      }
      scanJarFile(jarFile, file.toPath(), collector);
    }
  }

  private static void scanJarFile(JarFile file, Path path, Collection<Path> collector)
      throws IOException {
    try (FileSystem zipfs = FileSystems.newFileSystem(path, null)) {
      Enumeration<JarEntry> entries = file.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.isDirectory() || entry.getName().equals(JarFile.MANIFEST_NAME)) {
          continue;
        }
        collector.add(zipfs.getPath(entry.getName()));
      }
    }
  }

  private static Set<File> getClassPathFromManifest(File jarFile, Manifest manifest) {
    Set<File> result = new LinkedHashSet<>();

    if (manifest == null) {
      return result;
    }

    String classpathAttribute =
        manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH.toString());
    if (classpathAttribute == null) {
      return result;
    }

    for (String path : classpathAttribute.split(CLASSPATH_ATTIBUTE_MANIFEST_SEPARATOR)) {
      URL url;
      try {
        url = new URL(jarFile.toURI().toURL(), path);
      } catch (MalformedURLException e) {
        throw ThrowableUtil.propagate(e);
      }
      if (url.getProtocol().equals("file")) {
        result.add(toFile(url));
      }
    }
    return result;
  }

  private static void scanFrom(File file, Collection<Path> collector) throws IOException {
    try {
      if (!file.exists()) {
        return;
      }
    } catch (SecurityException e) {
      throw ThrowableUtil.propagate(e);
    }
    if (file.isDirectory()) {
      Set<File> currentPath = new HashSet<>(Collections.singleton(file.getCanonicalFile()));
      scanDirectory(file, "", currentPath, collector);
    } else {
      scanJar(file, collector);
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ClassPathConfigSource.class.getSimpleName() + "[", "]")
        .add("classLoader=" + getClass().getClassLoader())
        .toString();
  }
}
