package io.scalecube.config.audit;

import java.util.Collection;

/** Listener of configuration changes events. */
public interface ConfigEventListener {

  /** Process configuration change event. */
  void onEvents(Collection<ConfigEvent> event);
}
