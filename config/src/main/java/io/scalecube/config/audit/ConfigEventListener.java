package io.scalecube.config.audit;

/**
 * Listener of configuration changes events.
 */
public interface ConfigEventListener {

  /**
   * Process configuration change event.
   */
  void onEvent(ConfigEvent event);
}
