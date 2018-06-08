package io.scalecube.config.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

public class Slf4JConfigEventListener implements ConfigEventListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(Slf4JConfigEventListener.class);

  @Override
  public void onEvents(Collection<ConfigEvent> events) {
    if (!events.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      events.stream().sorted(Comparator.comparing(ConfigEvent::getName)).forEach(event -> {
        sb.append("\n");
        sb.append(event.getName()).append("=");
        sb.append(propValueAsString(event));
        sb.append(",\t");
        sb.append("source=");
        sb.append(sourceAsString(event));
        sb.append(",\t");
        sb.append("origin=");
        sb.append(originAsString(event));
      });
      sb.append("\n").append("]");
      LOGGER.info("Config property changed: {}", sb);
    }
  }

  private String originAsString(ConfigEvent event) {
    if (Objects.equals(event.getOldOrigin(), event.getNewOrigin())) {
      return event.getNewOrigin();
    } else {
      return event.getOldOrigin() + "->" + event.getNewOrigin();
    }
  }

  private String sourceAsString(ConfigEvent event) {
    if (Objects.equals(event.getOldSource(), event.getNewSource())) {
      return event.getNewSource();
    } else {
      return event.getOldSource() + "->" + event.getNewSource();
    }
  }

  private String propValueAsString(ConfigEvent event) {
    if (event.getOldValue() != null && event.getNewValue() != null) {
      return "***->***";
    } else if (event.getOldValue() != null) {
      return "***->null";
    } else if (event.getNewValue() != null) {
      return "null->***";
    } else {
      return null;
    }
  }
}
