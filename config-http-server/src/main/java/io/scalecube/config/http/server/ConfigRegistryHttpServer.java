package io.scalecube.config.http.server;

import io.scalecube.config.ConfigRegistry;
import java.net.URI;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigRegistryHttpServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRegistryHttpServer.class);

  private final ConfigRegistry configRegistry;
  private final int port;

  private ConfigRegistryHttpServer(ConfigRegistry configRegistry, int port) {
    this.configRegistry = configRegistry;
    this.port = port;
  }

  /**
   * Creates http server for given {@link ConfigRegistry} and port.
   *
   * @param configRegistry config registry
   * @param port listen port
   * @return server instance
   */
  public static ConfigRegistryHttpServer create(ConfigRegistry configRegistry, int port) {
    ConfigRegistryHttpServer server = new ConfigRegistryHttpServer(configRegistry, port);
    server.start();
    return server;
  }

  private void start() {
    URI uri;
    try {
      uri = URI.create("http://0.0.0.0:" + port + "/");
      ResourceConfig resourceConfig =
          new ResourceConfig(JacksonFeature.class, ObjectMapperProvider.class);
      resourceConfig.register(RolesAllowedDynamicFeature.class);
      resourceConfig.register(new ConfigRegistryResource(configRegistry));
      Server server = JettyHttpContainerFactory.createServer(uri, resourceConfig, true /* start */);
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  () -> {
                    try {
                      server.stop();
                    } catch (Exception e) {
                      LOGGER.warn("Exception occurred on stop of {}, cause: {}", toString(), e);
                    }
                  }));
      LOGGER.info("Started: {}", toString());
    } catch (Exception e) {
      LOGGER.warn("Exception on start of {}, cause: {}", toString(), e);
    }
  }

  @Override
  public String toString() {
    return "ConfigRegistryHttpServer {port=" + port + '}';
  }
}
