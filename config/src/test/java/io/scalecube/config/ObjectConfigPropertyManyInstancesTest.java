package io.scalecube.config;

import static io.scalecube.config.TestUtil.WAIT_FOR_RELOAD_PERIOD_MILLIS;
import static io.scalecube.config.TestUtil.mapBuilder;
import static io.scalecube.config.TestUtil.newConfigRegistry;
import static io.scalecube.config.TestUtil.toConfigProps;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.scalecube.config.source.ConfigSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class ObjectConfigPropertyManyInstancesTest {

  @Mock
  private ConfigSource configSource;
  @Mock
  private SideEffect sideEffect1;
  @Mock
  private SideEffect sideEffect2;

  // Normal scenarios

  @Test
  void testValueNullInitially() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder().build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<MailSettings> mailProperty =
        configRegistry.objectProperty("com.acme", MailSettings.class);
    assertFalse(mailProperty.value().isPresent());

    ObjectConfigProperty<ConnectorSettings> connectorProperty =
        configRegistry.objectProperty("com.acme", ConnectorSettings.class);
    assertFalse(connectorProperty.value().isPresent());
  }

  @Test
  void testReloadValueBecameNotNull() throws Exception {
    when(configSource.loadConfig())
        .thenReturn(toConfigProps(mapBuilder().build()))
        .thenReturn(toConfigProps(mapBuilder()
            .put("com.acme.accessKey", "access")
            .put("com.acme.secretKey", "secret")
            .put("com.acme.emailFrom", "email@email.com")
            .put("com.acme.connectUrl", "protocol://admi@admin?connecthere")
            .build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<MailSettings> mailProperty =
        configRegistry.objectProperty("com.acme", MailSettings.class);
    mailProperty.addCallback((d1, d2) -> sideEffect1.apply(d1, d2));
    assertFalse(mailProperty.value().isPresent());

    ObjectConfigProperty<ConnectorSettings> connectorProperty =
        configRegistry.objectProperty("com.acme", ConnectorSettings.class);
    connectorProperty.addCallback((s1, s2) -> sideEffect2.apply(s1, s2));
    assertFalse(connectorProperty.value().isPresent());

    TimeUnit.MILLISECONDS.sleep(WAIT_FOR_RELOAD_PERIOD_MILLIS);

    assertTrue(mailProperty.value().isPresent());
    ArgumentCaptor<MailSettings> mailSettingsArgumentCaptor = ArgumentCaptor.forClass(MailSettings.class);
    verify(sideEffect1).apply(any(), mailSettingsArgumentCaptor.capture());
    assertEquals("access", mailSettingsArgumentCaptor.getValue().accessKey);
    assertEquals("secret", mailSettingsArgumentCaptor.getValue().secretKey);
    assertEquals("email@email.com", mailSettingsArgumentCaptor.getValue().emailFrom);

    assertTrue(connectorProperty.value().isPresent());
    ArgumentCaptor<ConnectorSettings> connectorSettingsArgumentCaptor =
        ArgumentCaptor.forClass(ConnectorSettings.class);
    verify(sideEffect2).apply(any(), connectorSettingsArgumentCaptor.capture());
    assertEquals("access", connectorSettingsArgumentCaptor.getValue().accessKey);
    assertEquals("secret", connectorSettingsArgumentCaptor.getValue().secretKey);
    assertEquals("protocol://admi@admin?connecthere", connectorSettingsArgumentCaptor.getValue().connectUrl);
  }

  // Failure scenarios

  @Test
  void testValidationNotPassedAtOne() {
    when(configSource.loadConfig()).thenReturn(toConfigProps(mapBuilder()
        .put("com.acme.accessKey", "access")
        .put("com.acme.secretKey", "secret")
        .put("com.acme.emailFrom", "email@email.com")
        .put("com.acme.connectUrl", "protocol://admi@admin?connecthere")
        .build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<MailSettings> mailProperty =
        configRegistry.objectProperty("com.acme", MailSettings.class);
    mailProperty.addValidator(settings -> settings.emailFrom.contains("@")); // validation is passing
    assertTrue(mailProperty.value().isPresent());

    ObjectConfigProperty<ConnectorSettings> connectorProperty =
        configRegistry.objectProperty("com.acme", ConnectorSettings.class);

    assertThrows(IllegalArgumentException.class,
        () -> connectorProperty.addValidator(settings -> settings.connectUrl.startsWith("http")),
        "Validation failed");
  }

  public interface SideEffect {
    boolean apply(Object t1, Object t2);
  }

  public static class MailSettings {
    String accessKey;
    String secretKey;
    String emailFrom;
  }

  public static class ConnectorSettings {
    String accessKey;
    String secretKey;
    String connectUrl;
  }
}
