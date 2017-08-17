package io.scalecube.config.http.server;

import io.scalecube.config.ConfigPropertyInfo;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.audit.ConfigEvent;
import io.scalecube.config.source.ConfigSourceInfo;

import java.util.Collection;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/_config/")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll // TODO: Require basic auth here instead (see @RolesAllowed)
public class ConfigRegistryResource {
  private final ConfigRegistry configRegistry;

  public ConfigRegistryResource(ConfigRegistry configRegistry) {
    this.configRegistry = configRegistry;
  }

  @GET
  @Path("sources")
  public Collection<ConfigSourceInfo> getSources() {
    return configRegistry.getConfigSources();
  }

  @GET
  @Path("properties")
  public Collection<ConfigPropertyInfo> getProperties() {
    return configRegistry.getConfigProperties();
  }

  @GET
  @Path("events")
  public Collection<ConfigEvent> getEvents() {
    return configRegistry.getRecentConfigEvents();
  }

  @GET
  @Path("settings")
  public ConfigRegistrySettings getSettings() {
    return configRegistry.getSettings();
  }
}
