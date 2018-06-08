package io.scalecube.config.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class Slf4JConfigEventListener implements ConfigEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(Slf4JConfigEventListener.class);

  @Override
  public void onEvents(Collection<AuditConfigEvent> events) {
    LOGGER.info("Config property changed: {}", events);
  }

}
