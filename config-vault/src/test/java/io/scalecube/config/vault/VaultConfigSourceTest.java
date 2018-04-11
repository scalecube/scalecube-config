package io.scalecube.config.vault;

import static co.unruly.matchers.OptionalMatchers.contains;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.StringConfigProperty;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.vault.VaultContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VaultConfigSourceTest {

  private static final String VAULT_IMAGE_NAME = "vault:0.9.6";
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

  static WaitStrategy VAULT_SERVER_STARTED =
      new LogMessageWaitStrategy().withRegEx("==> Vault server started! Log data will stream in below:\n").withTimes(1);

  private final Pattern unsealKeyPattern = Pattern.compile("Unseal Key: ([a-z/0-9=A-Z]*)\n");

  private Consumer<OutputFrame> waitingForUnsealKey(AtomicReference<String> unsealKey) {
    return onFrame -> {
      Matcher matcher = unsealKeyPattern.matcher(onFrame.getUtf8String());
      if (matcher.find()) {
        unsealKey.set(matcher.group(1));
      }
    };
  }

  @ClassRule
  public static VaultContainer<?> vaultContainer = new VaultContainer<>()
      .waitingFor(VAULT_SERVER_STARTED).withVaultToken(VAULT_TOKEN)
      .withVaultPort(VAULT_PORT)
      .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
      .withSecretInVault(VAULT_SECRETS_PATH2, "top_secret=password2", "db_password=dbpassword2")
      .withSecretInVault(VAULT_SECRETS_PATH3, "secret=password", "password=dbpassword");

  EnvironmentLoader loader1, loader2, loader3;

  Map<String, String> commonEnvironmentVariables = new HashMap<>();

  @Before
  public void setUp() throws Exception {
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


  }

  private class MockEnvironmentLoader extends EnvironmentLoader {
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
    assertThat(actual.name(), equalTo("top_secret"));
    assertThat(actual.valueAsString(""), equalTo("password1"));
  }

  @Test
  public void testSecondTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader2);
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    ConfigProperty actual = loadConfig.get("top_secret");
    assertThat(actual, notNullValue());
    assertThat(actual.name(), equalTo("top_secret"));
    assertThat(actual.valueAsString(""), equalTo("password2"));
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
    EnvironmentLoader loader4;
    Map<String, String> tenant4 = new HashMap<>(commonEnvironmentVariables);
    tenant4.put(VAULT_SECRETS_PATH, "secrets/unknown/path");
    loader4 = new MockEnvironmentLoader(tenant4);
    VaultConfigSource vaultConfigSource = new VaultConfigSource(loader4);
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    assertThat(loadConfig.size(), equalTo(0));
  }

  @Test(expected = ConfigSourceNotAvailableException.class)
  public void testInvalidAddress() {
    Map<String, String> invalidAddress = new HashMap<>();
    invalidAddress.put("VAULT_ADDR", "http://invalid.host.local:8200");
    invalidAddress.put("VAULT_TOKEN", VAULT_TOKEN);
    invalidAddress.put(VAULT_SECRETS_PATH, VAULT_SECRETS_PATH1);

    VaultConfigSource vaultConfigSource = new VaultConfigSource(new MockEnvironmentLoader(invalidAddress));
    vaultConfigSource.loadConfig();

  }

  @Test(expected = ConfigSourceNotAvailableException.class)
  public void testInvalidToken() {
    Map<String, String> invalidToken = new HashMap<>(commonEnvironmentVariables);
    invalidToken.put("VAULT_TOKEN", "zzzzzz");
    invalidToken.put(VAULT_SECRETS_PATH, "secrets/unknown/path");

    VaultConfigSource vaultConfigSource = new VaultConfigSource(new MockEnvironmentLoader(invalidToken));
    vaultConfigSource.loadConfig();

  }

  @Test
  public void shouldWorkWhenRegistryIsReloadedAndVaultIsRunning() throws InterruptedException {
    try (VaultContainer<?> vaultContainer2 = new VaultContainer<>(VAULT_IMAGE_NAME)) {
      vaultContainer2.withVaultToken(VAULT_TOKEN).withVaultPort(8202)
          .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
          .waitingFor(VAULT_SERVER_STARTED)
          .start();
      String address = new StringBuilder("http://")
          .append(vaultContainer2.getContainerIpAddress()).append(':').append(8202).toString();
      ConfigRegistrySettings settings = ConfigRegistrySettings.builder()
          .addLastSource("vault", VaultConfigSource.builder(address, VAULT_TOKEN, VAULT_SECRETS_PATH1).build())
          .reloadIntervalSec(1)
          .build();
      ConfigRegistry configRegistry = ConfigRegistry.create(settings);
      StringConfigProperty configProperty = configRegistry.stringProperty("top_secret");

      assertThat(configProperty.value(), contains("password1"));
      try {
        ExecResult execResult = vaultContainer2.execInContainer("/bin/sh", "-c",
            "vault write " + VAULT_SECRETS_PATH1 + " top_secret=new_password");
        assumeThat(execResult.getStdout(), CoreMatchers.containsString("Success"));
        TimeUnit.SECONDS.sleep(2);
      } catch (Exception ignoredException) {
        Assert.fail("oops");
      }
      assertThat(configProperty.value(), contains("new_password"));
    }
  }

  @Test
  public void shouldWorkWhenRegistryIsReloadedAndVaultIsDown() {
    String PASSWORD_PROPERTY_NAME = "password";
    String PASSWORD_PROPERTY_VALUE = "123456";
    String secret = PASSWORD_PROPERTY_NAME + "=" + PASSWORD_PROPERTY_VALUE;
    try (VaultContainer<?> vaultContainer2 = new VaultContainer<>(VAULT_IMAGE_NAME)) {
      vaultContainer2.withVaultToken(VAULT_TOKEN).withVaultPort(8203)
          .withEnv("VAULT_DEV_ROOT_TOKEN_ID", (String) VAULT_TOKEN)
          .withSecretInVault(VAULT_SECRETS_PATH1, secret)
          .waitingFor(VAULT_SERVER_STARTED)
          .start();

      String address = new StringBuilder("http://")
          .append(vaultContainer2.getContainerIpAddress()).append(':').append(8203).toString();

      ConfigRegistrySettings settings = ConfigRegistrySettings.builder()
          .addLastSource("vault", VaultConfigSource.builder(address, VAULT_TOKEN, VAULT_SECRETS_PATH1).build())
          .reloadIntervalSec(1)
          .build();
      ConfigRegistry configRegistry = ConfigRegistry.create(settings);
      StringConfigProperty configProperty = configRegistry.stringProperty(PASSWORD_PROPERTY_NAME);
      configProperty.addValidator(Objects::nonNull);

      vaultContainer2.stop();
      assertFalse(vaultContainer2.isRunning());

      try {
        TimeUnit.SECONDS.sleep(2);
      } catch (InterruptedException ignoredException) {
      }

      assertThat(configProperty.value(), contains(PASSWORD_PROPERTY_VALUE));
    }
  }


  @Test
  public void testSealed() throws Throwable {
    try (VaultContainer<?> vaultContainerSealed = new VaultContainer<>()) {
      vaultContainerSealed.withVaultToken(VAULT_TOKEN).withVaultPort(8204)
          .waitingFor(VAULT_SERVER_STARTED)
          .start();

      String address = new StringBuilder("http://")
          .append(vaultContainerSealed.getContainerIpAddress()).append(':').append(8204).toString();
      Vault vault = new Vault(new VaultConfig().address(address).token(VAULT_TOKEN).sslConfig(new SslConfig()));

      vault.seal().seal();
      assumeTrue(vault.seal().sealStatus().getSealed());


      Map<String, String> clientEnv = new HashMap<>();
      clientEnv.put("VAULT_TOKEN", "ROOT");
      clientEnv.put("VAULT_ADDR", address);
      clientEnv.put(VAULT_SECRETS_PATH, VAULT_SECRETS_PATH1);

      new VaultConfigSource(new MockEnvironmentLoader(clientEnv)).loadConfig();
      Assert.fail("Negative test failed");
    } catch (ConfigSourceNotAvailableException expectedException) {
      assertThat(expectedException.getCause(), instanceOf(VaultException.class));
      String message = expectedException.getCause().getMessage();
      assertThat(message, containsString("Vault is sealed"));
    }
  }

  @Test
  public void shouldWorkWhenRegistryIsReloadedAndVaultIsUnSealed() throws InterruptedException {
    AtomicReference<String> unsealKey = new AtomicReference<>();
    try (VaultContainer<?> vaultContainer2 = new VaultContainer<>(VAULT_IMAGE_NAME)) {
      vaultContainer2.withVaultToken(VAULT_TOKEN).withVaultPort(8205)
          .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
          .withLogConsumer(waitingForUnsealKey(unsealKey)).waitingFor(VAULT_SERVER_STARTED)
          .start();
      assumeThat("unable to get unseal key", unsealKey.get(), notNullValue());
      String address = new StringBuilder("http://")
          .append(vaultContainer2.getContainerIpAddress()).append(':').append(8205).toString();
      ConfigRegistrySettings settings = ConfigRegistrySettings.builder()
          .addLastSource("vault", VaultConfigSource.builder(address, VAULT_TOKEN, VAULT_SECRETS_PATH1).build())
          .reloadIntervalSec(1)
          .build();

      ConfigRegistry configRegistry = ConfigRegistry.create(settings);
      StringConfigProperty configProperty = configRegistry.stringProperty("top_secret");

      assertThat("initial value of top_secret", configProperty.value(), contains("password1"));
      Vault vault = new Vault(new VaultConfig().address(address).token(VAULT_TOKEN).sslConfig(new SslConfig()));
      Map<String, Object> newValues = new HashMap<>();
      newValues.put(configProperty.name(), "new_password");

      try {
        vault.logical().write(VAULT_SECRETS_PATH1, newValues);
        vault.seal().seal();
        assumeThat("valut seal status", vault.seal().sealStatus().getSealed(), is(true));
      } catch (VaultException vaultException) {
        fail(vaultException.getMessage());
      }
      TimeUnit.SECONDS.sleep(2);
      assumeThat("new value was unexpectedly set", configProperty.value(), not(contains("new_password")));
      try {
        vault.seal().unseal(unsealKey.get());
        assumeThat("valut seal status", vault.seal().sealStatus().getSealed(), is(false));
      } catch (VaultException vaultException) {
        fail(vaultException.getMessage());
      }
      TimeUnit.SECONDS.sleep(2);
      assertThat(configProperty.value(), contains("new_password"));
    }
  }
}
