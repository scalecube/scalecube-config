package io.scalecube.config.audit;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

public class LoggingConfigEventListener implements ConfigEventListener {

  private static final Logger LOGGER = System.getLogger(LoggingConfigEventListener.class.getName());

  @Override
  public void onEvents(Collection<ConfigEvent> events) {
    if (!events.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      events.stream()
          .sorted(Comparator.comparing(ConfigEvent::getName))
          .forEach(
              event -> {
                sb.append("\n");
                sb.append(event.getName()).append("=[");
                sb.append(propValueAsString(event));
                sb.append("], ");
                sb.append("source=");
                sb.append(sourceAsString(event));
                sb.append(", ");
                sb.append("origin=");
                sb.append(originAsString(event));
              });
      sb.append("\n").append("]");
      LOGGER.log(Level.INFO, sb.toString());
    }
  }

  private static String originAsString(ConfigEvent event) {
    final String oldValue = event.getOldOrigin();
    final String newValue = event.getNewOrigin();

    if (Objects.equals(oldValue, newValue)) {
      return newValue;
    }
    return (oldValue == null || oldValue.isEmpty()) ? newValue : oldValue + "->" + newValue;
  }

  private static String sourceAsString(ConfigEvent event) {
    final String oldValue = event.getOldSource();
    final String newValue = event.getNewSource();

    if (Objects.equals(oldValue, newValue)) {
      return newValue;
    }
    return (oldValue == null || oldValue.isEmpty()) ? newValue : oldValue + "->" + newValue;
  }

  private static String propValueAsString(ConfigEvent event) {
    final String oldValue = event.getOldValue();
    final String newValue = event.getNewValue();

    if (Objects.equals(oldValue, newValue)) {
      return newValue;
    }
    return (oldValue == null || oldValue.isEmpty())
        ? mask(newValue)
        : mask(oldValue) + "->" + mask(newValue);
  }

  private static String mask(String value) {
    if (value == null || value.isEmpty()) {
      return "null";
    }
    if (value.length() < 5) {
      return "***";
    }
    return value.replace(value.substring(2, value.length() - 2), "***");
  }
}
