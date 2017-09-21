# ScaleCube Config

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.scalecube/config/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.scalecube/config)

ScaleCube Config is a configuration management library for JVM based distributed applications.

It provides the following functionality:
* Dynamic typed properties
* Register callbacks on property changes
* Object binding for grouping properties
* Extensible range of supported property sources (environment variables, program arguments, classpath, property files, mongodb, git repository, zookeeper etc.)
* Support centralized hierarchical property sources
* Control over order of applying different property sources
* Audit log of property changes
* Expose properties and settings via JMX and/or HTTP

## Usage

Configure and xreate configuration registry instance:

``` java
Predicate<Path> predicate = path -> path.toString().endsWith(".props"); // match by .props extension
ConfigRegistrySettings settings = ConfigRegistrySettings.builder()
        .addLastSource("classpath", new ClassPathConfigSource(predicate))
        .addLastSource("configDirectory", new DirectoryConfigSource("conf" /* base path */, predicate))
        .addListener(new Slf4JConfigEventListener()) // print all property changes to log
        .build();
ConfigRegistry configRegistry = ConfigRegistry.create(settings);
```

Get dynamic typed configuration property:

``` java
LongConfigProperty timeoutProperty = configRegistry.longProperty("http.request-timeout");
long timeout = timeoutProperty.get(30 /* default value */);
```

Register callbacks on property modifications:
 
``` java
timeoutProperty.addCallback((oldValue, newValue) -> 
        System.out.println("Timeout value changed to " + newValue));
```

Utilize object binding:

``` java
// Define configuration class
public interface MyConfig {
  private boolean featureFlag;
  private int someValue;
  private double realValue;
  ...
}

// myapp.config.featureFlag=true
// myapp.config.someValue=42
// myapp.config.realValue=36.6
ObjectConfigProperty<MyConfig> config = configRegistry.objectProperty("myapp.config", MyConfig.class);

// Get current config values
MyConfig currentConfig = config.value(MyConfig.defaultValue() /* or default */);

// Register callback (called only once per config reload even when several properties changed)
config.addCallback((oldConfig, newConfig) -> 
        System.out.println("Config was changed to: " + newConfig)); 
```

Start embedded HTTP server which exposes configuration endpoints:
  
``` java
ConfigRegistryHttpServer.create(configRegistry, 5050); // starts http server on port 5050
```

After HTTP server is started explore configuration registry by browsing following endpoints: 
* [http://localhost:5050/_config/properties](http://localhost:5050/_config/properties)
* [http://localhost:5050/_config/sources](http://localhost:5050/_config/sources)
* [http://localhost:5050/_config/events](http://localhost:5050/_config/events)
* [http://localhost:5050/_config/settings](http://localhost:5050/_config/settings)

See more examples at [config-examples](https://github.com/scalecube/config/tree/master/config-examples/src/main/java/io/scalecube/config/examples) module.

## Maven 

Binaries and dependency information for Maven can be found at 
[http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cio.scalecube.config).

Change history and [version numbers](http://semver.org/) can be found at [CHANGES.md](https://github.com/scalecube/config/blob/master/CHANGES.md). 

Maven dependency: 

``` xml
<dependency>
  <groupId>io.scalecube</groupId>
  <artifactId>config</artifactId>
  <version>x.y.z</version>
</dependency>

<!-- For exposing config HTTP endpoints -->
<dependency>
  <groupId>io.scalecube</groupId>
  <artifactId>config-http-server</artifactId>
  <version>x.y.z</version>
</dependency>

<!-- For MongoDB integration (beta version) -->
<dependency>
  <groupId>io.scalecube</groupId>
  <artifactId>config-mongo</artifactId>
  <version>x.y.z</version>
</dependency>

```

## Bugs and Feedback

For bugs, questions and discussions please use the [GitHub Issues](https://github.com/scalecube/config/issues).

## License

[Apache License, Version 2.0](https://github.com/scalecube/config/blob/master/LICENSE.txt)
