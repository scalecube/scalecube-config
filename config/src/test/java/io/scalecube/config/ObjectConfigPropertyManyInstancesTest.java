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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ObjectConfigPropertyManyInstancesTest {

  @Mock private ConfigSource configSource;
  @Mock private SideEffect sideEffect1;
  @Mock private SideEffect sideEffect2;

  // Normal scenarios

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  void testMultipleInstancesWithSamePrefix() {
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    .put("com.acme.accessKey", "access")
                    .put("com.acme.secretKey", "secret")
                    .put("com.acme.emailFrom", "email@email.com")
                    .put("com.acme.connectUrl", "protocol://admi@admin?connecthere")
                    .build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    MailSettings mailPropertyFoo =
        configRegistry.objectProperty("com.acme", MailSettings.class).value().get();
    ConnectorSettings connectorPropertyFoo =
        configRegistry.objectProperty("com.acme", ConnectorSettings.class).value().get();

    assertEquals("access", mailPropertyFoo.accessKey);
    assertEquals("secret", mailPropertyFoo.secretKey);
    assertEquals("email@email.com", mailPropertyFoo.emailFrom);

    assertEquals("access", connectorPropertyFoo.accessKey);
    assertEquals("secret", connectorPropertyFoo.secretKey);
    assertEquals("protocol://admi@admin?connecthere", connectorPropertyFoo.connectUrl);

    MailSettings mailPropertyBar =
        configRegistry.objectProperty("com.acme", MailSettings.class).value().get();
    ConnectorSettings connectorPropertyBar =
        configRegistry.objectProperty("com.acme", ConnectorSettings.class).value().get();

    assertEquals("access", mailPropertyBar.accessKey);
    assertEquals("secret", mailPropertyBar.secretKey);
    assertEquals("email@email.com", mailPropertyBar.emailFrom);

    assertEquals("access", connectorPropertyBar.accessKey);
    assertEquals("secret", connectorPropertyBar.secretKey);
    assertEquals("protocol://admi@admin?connecthere", connectorPropertyBar.connectUrl);
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Test
  void testMultipleInstancesWithDifferentPrefix() {
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    // default prefix
                    .put("com.acme.accessKey", "access")
                    .put("com.acme.secretKey", "secret")
                    .put("com.acme.emailFrom", "email@email.com")
                    // backup prefix
                    .put("com.acme.backup.accessKey", "access_backup")
                    .put("com.acme.backup.secretKey", "secret_backup")
                    .put("com.acme.backup.emailFrom", "email@email.com_backup")
                    // primary prefix
                    .put("com.acme.primary.accessKey", "access_primary")
                    .put("com.acme.primary.secretKey", "secret_primary")
                    .put("com.acme.primary.emailFrom", "email@email.com_primary")
                    .build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    MailSettings mailPropertyDefault =
        configRegistry.objectProperty("com.acme", MailSettings.class).value().get();
    MailSettings mailPropertyBackup =
        configRegistry.objectProperty("com.acme.backup", MailSettings.class).value().get();
    MailSettings mailPropertyPrimary =
        configRegistry.objectProperty("com.acme.primary", MailSettings.class).value().get();

    assertEquals("access", mailPropertyDefault.accessKey);
    assertEquals("secret", mailPropertyDefault.secretKey);
    assertEquals("email@email.com", mailPropertyDefault.emailFrom);

    assertEquals("access_backup", mailPropertyBackup.accessKey);
    assertEquals("secret_backup", mailPropertyBackup.secretKey);
    assertEquals("email@email.com_backup", mailPropertyBackup.emailFrom);

    assertEquals("access_primary", mailPropertyPrimary.accessKey);
    assertEquals("secret_primary", mailPropertyPrimary.secretKey);
    assertEquals("email@email.com_primary", mailPropertyPrimary.emailFrom);
  }

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
        .thenReturn(
            toConfigProps(
                mapBuilder()
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
    ArgumentCaptor<MailSettings> mailSettingsArgumentCaptor =
        ArgumentCaptor.forClass(MailSettings.class);
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
    assertEquals(
        "protocol://admi@admin?connecthere", connectorSettingsArgumentCaptor.getValue().connectUrl);
  }

  // Failure scenarios

  @Test
  void testValidationNotPassedAtOne() {
    when(configSource.loadConfig())
        .thenReturn(
            toConfigProps(
                mapBuilder()
                    .put("com.acme.accessKey", "access")
                    .put("com.acme.secretKey", "secret")
                    .put("com.acme.emailFrom", "email@email.com")
                    .put("com.acme.connectUrl", "protocol://admi@admin?connecthere")
                    .build()));
    ConfigRegistryImpl configRegistry = newConfigRegistry(configSource);

    ObjectConfigProperty<MailSettings> mailProperty =
        configRegistry.objectProperty("com.acme", MailSettings.class);
    mailProperty.addValidator(
        settings -> settings.emailFrom.contains("@")); // validation is passing
    assertTrue(mailProperty.value().isPresent());

    ObjectConfigProperty<ConnectorSettings> connectorProperty =
        configRegistry.objectProperty("com.acme", ConnectorSettings.class);

    assertThrows(
        IllegalArgumentException.class,
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
