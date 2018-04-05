package io.scalecube.config.vault;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.StringConfigProperty;

import com.bettercloud.vault.EnvironmentLoader;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.vault.VaultContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TestVaultConfigSource {

  private static final int VAULT_PORT = 8200;
  private static final String VAULT_TOKEN = "my-root-token";
  /**
   * the environment variable name for vault secret path
   */
  private static final String VAULT_SECRETS_PATH = "VAULT_SECRETS_PATH";

  private static final String VAULT_SECRETS_PATH1 = "secret/application/tenant1";
  private static final String VAULT_SECRETS_PATH2 = "secret/application/tenant2";
  private static final String VAULT_SECRETS_PATH3 = "secret/application2/tenant3";
  private static final String SECRETS_PATH = "secret";

  private static final String PASSWORD_PROPERTY_NAME = "password";
  private static final String PASSWORD_PROPERTY_VALUE = "123456";

  @ClassRule
  public static VaultContainer<?> vaultContainer = new VaultContainer<>()
      .withVaultToken(VAULT_TOKEN)
      .withVaultPort(VAULT_PORT)
      .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
      .withSecretInVault(VAULT_SECRETS_PATH2, "top_secret=password2", "db_password=dbpassword2")
      .withSecretInVault(VAULT_SECRETS_PATH3, "secret=password", "password=dbpassword")
      .withSecretInVault(SECRETS_PATH, PASSWORD_PROPERTY_NAME + "=" + PASSWORD_PROPERTY_VALUE);

  private static final String VAULT_ADDRESS = "http://" + vaultContainer.getContainerIpAddress() + ":" + VAULT_PORT;

  private EnvironmentLoader loader1, loader2, loader3, loader4;

  @Before
  public void setUp() {
    if (!vaultContainer.isRunning()) {
      vaultContainer.start();
    }

    Map<String, String> commonEnvironmentVariables = new HashMap<>();
    commonEnvironmentVariables.put("VAULT_TOKEN", VAULT_TOKEN);
    commonEnvironmentVariables.put("VAULT_ADDR", "http://" + vaultContainer.getContainerIpAddress() + ':' + VAULT_PORT);

    Map<String, String> tenant1 = new HashMap<>(commonEnvironmentVariables);
    tenant1.put(VAULT_SECRETS_PATH, VAULT_SECRETS_PATH1);
    this.loader1 = new MockEnvironmentLoader(tenant1);

    Map<String, String> tenant2 = new HashMap<>(commonEnvironmentVariables);
    tenant2.put(VAULT_SECRETS_PATH, VAULT_SECRETS_PATH2);
    this.loader2 = new MockEnvironmentLoader(tenant2);

    Map<String, String> tenant3 = new HashMap<>(commonEnvironmentVariables);
    tenant3.put(VAULT_SECRETS_PATH, VAULT_SECRETS_PATH3);
    this.loader3 = new MockEnvironmentLoader(tenant3);

    this.loader4 = new MockEnvironmentLoader(commonEnvironmentVariables);
  }

  private class MockEnvironmentLoader extends EnvironmentLoader {

    private final Map<String, String> delegate;

    MockEnvironmentLoader(Map<String, String> delegate) {
      this.delegate = delegate;
    }

    @Override
    public String loadVariable(String name) {
      return delegate.get(name);
    }
  }

  @Test
  public void testFirstTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader1);
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    ConfigProperty actual = loadConfig.get("top_secret");
    assertThat(actual, notNullValue());
    assertThat(actual.name(), is("top_secret"));
    assertThat(actual.valueAsString(""), is("password1"));
  }

  @Test
  public void testSecondTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader2);
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    ConfigProperty actual = loadConfig.get("top_secret");
    assertThat(actual, notNullValue());
    assertThat(actual.name(), is("top_secret"));
    assertThat(actual.valueAsString(""), is("password2"));
  }

  @Test
  public void testMissingProperty() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader3);
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    assertThat(loadConfig.size(), not(0));
    ConfigProperty actual = loadConfig.get("top_secret");
    assertThat(actual, nullValue());
  }

  @Test(expected = ConfigSourceNotAvailableException.class)
  public void testMissingTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader4);
    vaultConfigSource.loadConfig();
  }

  @Test(expected = ConfigSourceNotAvailableException.class)
  public void testInvalidAddress() {
    Map<String, String> map = new HashMap<>();
    map.put("VAULT_ADDR", "http://invalid.host:8200");
    VaultConfigSource vaultConfigSource = new VaultConfigSource(new MockEnvironmentLoader(map));
    vaultConfigSource.loadConfig();
  }

  @Test(expected = ConfigSourceNotAvailableException.class)
  public void testInvalidToken() {
    Map<String, String> map = new HashMap<>();
    map.put("VAULT_ADDR", "http://0.0.0.0:8200");
    map.put("VAULT_TOKEN", "invalid_token!");

    VaultConfigSource vaultConfigSource = new VaultConfigSource(new MockEnvironmentLoader(map));
    vaultConfigSource.loadConfig();
  }

  @Test
  public void shouldWorkWhenRegistryIsReloadedAndVaultIsRunning() throws InterruptedException {
    ConfigRegistrySettings settings = ConfigRegistrySettings.builder()
        .addLastSource("vault", VaultConfigSource.builder(VAULT_ADDRESS, VAULT_TOKEN, SECRETS_PATH).build())
        .reloadIntervalSec(1)
        .build();
    ConfigRegistry configRegistry = ConfigRegistry.create(settings);
    StringConfigProperty configProperty = configRegistry.stringProperty(PASSWORD_PROPERTY_NAME);

    TimeUnit.SECONDS.sleep(2);

    assertEquals(PASSWORD_PROPERTY_VALUE, configProperty.value().get());
  }

  @Test
  public void shouldWorkWhenRegistryIsReloadedAndVaultIsDown() throws InterruptedException {
    ConfigRegistrySettings settings = ConfigRegistrySettings.builder()
        .addLastSource("vault", VaultConfigSource.builder(VAULT_ADDRESS, VAULT_TOKEN, SECRETS_PATH).build())
        .reloadIntervalSec(1)
        .build();
    ConfigRegistry configRegistry = ConfigRegistry.create(settings);
    StringConfigProperty configProperty = configRegistry.stringProperty(PASSWORD_PROPERTY_NAME);
    configProperty.addValidator(Objects::nonNull);

    vaultContainer.stop();
    assertFalse(vaultContainer.isRunning());

    TimeUnit.SECONDS.sleep(2);

    assertEquals(PASSWORD_PROPERTY_VALUE, configProperty.value().get());
  }

}
