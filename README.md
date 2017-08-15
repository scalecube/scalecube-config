# ScaleCube Config

ScaleCube Config is a configuration management library for JVM based distributed applications.
It provides the following functionality:
* Dynamic typed properties
* Register callbacks on property changes
* Extensible range of supported property sources (environment variables, program arguments, classpath, property files, mongodb, git repository, zookeeper etc.)
* Support centralized hierarchical property sources
* Control over order of applying different property sources
* Audit log of property changes
* Expose properties and settings via JMX and/or HTTP

## Usage

Create configuration registry instance:

``` java
Predicate<Path> predicate = path -> path.toString().endsWith(".props"); // match files with .props extension
ConfigRegistrySettings settings = ConfigRegistrySettings.builder()
        .addLastSource("classpath", new ClassPathConfigSource(predicate))
        .addLastSource("configDirectory", new DirectoryConfigSource("." /* base path */, predicate))
        .addListener(new Slf4JConfigEventListener()) // print all property changes to log
        .build();
ConfigRegistry configRegistry = ConfigRegistry.create(settings);
```

Get dynamic typed configuration property:

``` java
LongConfigProperty timeoutProperty = configRegistry.longProperty("http.request-timeout");
long timeout = timeoutProperty.get(30 /* default value */);
```

Register callback on property changes:
 
``` java
timeoutProperty.addCallback((oldValue, newValue) -> System.out.println("Timeout value changed to " + newValue));
```

Start embedded HTTP server which exposes configuration endpoint:
  
``` java
ConfigRegistryHttpServer.create(configRegistry, 5050); // starts http server on port 5050
```

After HTTp server is started try to explore configuration registry by browsing following endpoints: 
* [/configuration/properties](http://localhost:5050/configuration/properties)
* [/configuration/sources](http://localhost:5050/configuration/sources)
* [/configuration/events](http://localhost:5050/configuration/events)
* [/configuration/settings](http://localhost:5050/configuration/settings)

See more examples at `config-examples` module.

## TODO

* clean up mongodb configuration source support
* support secured properties
* support of grouped callbacks
* support list and array property types
* support git repository configuration source
* support zookeeper configuration source

## Maven 

Binaries and dependency information for Maven can be found at 
[http://search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.scalecube%22%20AND%20a%3A%22config%22).

Change history and version numbers at [CHANGES.md](https://github.com/scalecube/config/blob/master/CHANGES.md).

Maven dependency: 

``` xml
<dependency>
  <groupId>io.scalecube</groupId>
  <artifactId>config</artifactId>
  <version>x.y.z</version>
</dependency>
```

// TBD

## Bugs and Feedback

For bugs, questions and discussions please use the [GitHub Issues](https://github.com/scalecube/config/issues).

## License

[Apache License, Version 2.0](https://github.com/scalecube/config/blob/master/LICENSE.txt)
