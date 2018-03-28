package io.scalecube.config.vault;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import io.scalecube.config.ConfigProperty;

import com.bettercloud.vault.EnvironmentLoader;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.vault.VaultContainer;

import java.util.HashMap;
import java.util.Map;

public class TestVaultConfigSource {

  private static final String VAULT_TOKEN = "my-root-token";
  private static final String VAULT_SECRETS_PATH = "secret/application/tenant";
  @ClassRule
  public static VaultContainer<?> vaultContainer = new VaultContainer<>()
      .withVaultToken(VAULT_TOKEN)
      .withVaultPort(8200)
      .withSecretInVault(VAULT_SECRETS_PATH, "top_secret=password1", "db_password=dbpassword1")
      .withSecretInVault(VAULT_SECRETS_PATH + "2", "top_secret=password2", "db_password=dbpassword2");

  EnvironmentLoader loader, loader2, loader3;


  @Before
  public void setUp() throws Exception {
    Map<String, String> overrides = new HashMap<>();
    overrides.put("VAULT_TOKEN", VAULT_TOKEN);
    overrides.put("VAULT_ADDR", "http://" + vaultContainer.getContainerIpAddress() + ":8200");
    overrides.put("VAULT_SECRETS_PATH", VAULT_SECRETS_PATH);
    this.loader = new MockEnvironmentLoader(overrides);

    Map<String, String> overrides2 = new HashMap<>(overrides);
    overrides2.put("VAULT_SECRETS_PATH", VAULT_SECRETS_PATH + "2");
    this.loader2 = new MockEnvironmentLoader(overrides2);

    Map<String, String> overrides3 = new HashMap<>(overrides2);
    overrides3.put("VAULT_SECRETS_PATH", VAULT_SECRETS_PATH + "3");
    this.loader3 = new MockEnvironmentLoader(overrides3);


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
  public void test() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader);
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
  public void testMissingTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader3);
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    ConfigProperty actual = loadConfig.get("top_secret");
    assertThat(actual, nullValue());

  }
}
