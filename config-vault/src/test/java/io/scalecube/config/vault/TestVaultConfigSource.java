package io.scalecube.config.vault;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.VaultException;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.vault.VaultContainer;

import java.util.HashMap;
import java.util.Map;

public class TestVaultConfigSource {

  private static final int VAULT_PORT = 8200;
  private static final String VAULT_TOKEN = "my-root-token";
  /**
   * the environment variable name for vault secret path
   */
  private static final String VAULT_SECRETS_PATH = "VAULT_SECRETS_PATH";

  // these 3 are actual values we would like to test with
  private static final String VAULT_SECRETS_PATH1 = "secret/application/tenant1";
  private static final String VAULT_SECRETS_PATH2 = "secret/application/tenant2";
  private static final String VAULT_SECRETS_PATH3 = "secret/application2/tenant3";
  private static final String VAULT_SECRETS_PATH4 = "secret";

  @ClassRule
  public static VaultContainer<?> vaultContainer = new VaultContainer<>()
      .withVaultToken(VAULT_TOKEN)
      .withVaultPort(VAULT_PORT)
      .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
      .withSecretInVault(VAULT_SECRETS_PATH2, "top_secret=password2", "db_password=dbpassword2")
      .withSecretInVault(VAULT_SECRETS_PATH3, "secret=password", "password=dbpassword");

  EnvironmentLoader loader1, loader2, loader3, loader4, loader5;


  @Before
  public void setUp() throws Exception {
    Map<String, String> commonEnvironmentVariables = new HashMap<>();
    commonEnvironmentVariables.put("VAULT_TOKEN", VAULT_TOKEN);
    commonEnvironmentVariables.put("VAULT_ADDR", new StringBuilder("http://")
        .append(vaultContainer.getContainerIpAddress()).append(':').append(VAULT_PORT).toString());

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

  @After
  public void tearDown() throws Exception {}

  class MockEnvironmentLoader extends EnvironmentLoader {
    /**
     * 
     */
    private static final long serialVersionUID = 7285747202838173640L;
    private final Map<String, String> delegate;

    public MockEnvironmentLoader(Map<String, String> delegate) {
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

  @Test
  public void testMissingTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader4);
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    assertThat(loadConfig.size(), is(0));
  }

  @Test(expected = ConfigSourceNotAvailableException.class)
  public void testInvalidAddress() {
    Map<String, String> map = new HashMap<>();
    map.put("VAULT_ADDR", "http://0.0.0.0:8200");
    map.put("VAULT_TOKEN", "abcd");
    VaultConfigSource vaultConfigSource = new VaultConfigSource(new MockEnvironmentLoader(map));
    vaultConfigSource.loadConfig();

  }

  @Test(expected = ConfigSourceNotAvailableException.class)
  public void testInvalidToken() {
    Map<String, String> map = new HashMap<>();
    map.put("VAULT_ADDR", "http://0.0.0.0:8200");

    VaultConfigSource vaultConfigSource = new VaultConfigSource(new MockEnvironmentLoader(map));
    vaultConfigSource.loadConfig();

  }

  @Test(expected = VaultException.class)
  public void testUninitialized() {
    VaultContainer<?> vaultContainer2 = new VaultContainer<>().withVaultToken(VAULT_TOKEN).withVaultPort(8201);
    vaultContainer2.withEnv("VAULT_DEV_ROOT_TOKEN_ID", (String) null);

    Map<String, String> clientEnv = new HashMap<>();
    clientEnv.put("VAULT_TOKEN", VAULT_TOKEN);
    clientEnv.put("VAULT_ADDR", new StringBuilder("http://")
        .append(vaultContainer2.getContainerIpAddress()).append(':').append(8201).toString());

    try {
      vaultContainer2.start();
      new VaultConfigSource(new MockEnvironmentLoader(clientEnv));
    } finally {
      vaultContainer2.stop();
    }

  }
}
