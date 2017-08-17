package io.scalecube.config.http.server;

import io.scalecube.config.ConfigRegistry;

import io.netty.channel.Channel;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.netty.httpserver.NettyHttpContainerProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class ConfigRegistryHttpServer {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRegistryHttpServer.class);

  private final ConfigRegistry configRegistry;
  private final int port;

  private ConfigRegistryHttpServer(ConfigRegistry configRegistry, int port) {
    this.configRegistry = configRegistry;
    this.port = port;
  }

  public static ConfigRegistryHttpServer create(ConfigRegistry configRegistry, int port) {
    ConfigRegistryHttpServer server = new ConfigRegistryHttpServer(configRegistry, port);
    server.start();
    return server;
  }

  private void start() {
    URI uri;
    try {
      uri = URI.create("http://0.0.0.0:" + port + "/");
      ResourceConfig resourceConfig = new ResourceConfig(JacksonFeature.class, ObjectMapperProvider.class);
      resourceConfig.register(RolesAllowedDynamicFeature.class);
      resourceConfig.register(new ConfigRegistryResource(configRegistry));
      Channel server = NettyHttpContainerProvider.createHttp2Server(uri, resourceConfig, null);
      Runtime.getRuntime().addShutdownHook(new Thread(server::close));
      LOGGER.info("Started: {}", toString());
    } catch (Exception e) {
      LOGGER.warn("Exception occurred on start of {}, cause: {}", toString(), e);
    }
  }

  @Override
  public String toString() {
    return "ConfigRegistryHttpServer {port=" + port + '}';
  }
}
