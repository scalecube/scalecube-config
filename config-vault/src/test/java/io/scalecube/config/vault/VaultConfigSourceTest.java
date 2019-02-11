package io.scalecube.config.vault;

import static io.scalecube.config.vault.VaultContainerExtension.VAULT_IMAGE_NAME;
import static io.scalecube.config.vault.VaultContainerExtension.VAULT_PORT;
import static io.scalecube.config.vault.VaultContainerExtension.VAULT_SECRETS_PATH;
import static io.scalecube.config.vault.VaultContainerExtension.VAULT_SECRETS_PATH1;
import static io.scalecube.config.vault.VaultContainerExtension.VAULT_SECRETS_PATH2;
import static io.scalecube.config.vault.VaultContainerExtension.VAULT_SECRETS_PATH3;
import static io.scalecube.config.vault.VaultContainerExtension.VAULT_SERVER_STARTED;
import static io.scalecube.config.vault.VaultContainerExtension.VAULT_TOKEN;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigRegistry;
import io.scalecube.config.ConfigRegistrySettings;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.StringConfigProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.vault.VaultContainer;

class VaultConfigSourceTest {

  private static final Pattern unsealKeyPattern = Pattern.compile("Unseal Key: ([a-z/0-9=A-Z]*)\n");

  private Map<String, String> environmentVariables = new HashMap<>();

  @RegisterExtension
  static final VaultContainerExtension vaultContainerExtension = new VaultContainerExtension();

  private Consumer<OutputFrame> waitingForUnsealKey(AtomicReference<String> unsealKey) {
    return onFrame -> {
      Matcher matcher = unsealKeyPattern.matcher(onFrame.getUtf8String());
      if (matcher.find()) {
        unsealKey.set(matcher.group(1));
      }
    };
  }

  @BeforeEach
  void setUp() {
    environmentVariables.put("VAULT_TOKEN", VAULT_TOKEN);
    environmentVariables.put(
        "VAULT_ADDR",
        "http://" + vaultContainerExtension.container().getContainerIpAddress() + ':' + VAULT_PORT);
  }

  @Test
  void testFirstTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(
        environmentVariables.get("VAULT_ADDR"),
        environmentVariables.get("VAULT_TOKEN"),
        VAULT_SECRETS_PATH1
    );
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    ConfigProperty actual = loadConfig.get("top_secret");

    assertThat(actual, notNullValue());
    assertThat(actual.name(), equalTo("top_secret"));
    assertThat(actual.valueAsString(""), equalTo("password1"));
  }

  @Test
  void testSecondTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(
        environmentVariables.get("VAULT_ADDR"),
        environmentVariables.get("VAULT_TOKEN"),
        VAULT_SECRETS_PATH2
    );
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();
    ConfigProperty actual = loadConfig.get("top_secret");

    assertThat(actual, notNullValue());
    assertThat(actual.name(), equalTo("top_secret"));
    assertThat(actual.valueAsString(""), equalTo("password2"));
  }

  @Test
  void testMissingProperty() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(
        environmentVariables.get("VAULT_ADDR"),
        environmentVariables.get("VAULT_TOKEN"),
        VAULT_SECRETS_PATH3
    );
    Map<String, ConfigProperty> loadConfig = vaultConfigSource.loadConfig();

    assertThat(loadConfig.size(), not(0));

    ConfigProperty actual = loadConfig.get("top_secret");
    assertThat(actual, nullValue());
  }

  @Test
  void testMissingTenant() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(
        environmentVariables.get("VAULT_ADDR"),
        environmentVariables.get("VAULT_TOKEN"),
        "secrets/unknown/path"
    );

    assertThrows(ConfigSourceNotAvailableException.class, vaultConfigSource::loadConfig);
  }

  @Test
  void testInvalidAddress() {
    VaultConfigSource vaultConfigSource = new VaultConfigSource(
        "http://invalid.host.local:8200",
        environmentVariables.get("VAULT_TOKEN"),
        VAULT_SECRETS_PATH1
    );
    assertThrows(ConfigSourceNotAvailableException.class, vaultConfigSource::loadConfig);
  }

  @Test
  void testInvalidToken() {

    assertThrows(ConfigSourceNotAvailableException.class,
        () -> new VaultConfigSource(
            "zzzzzz",
            environmentVariables.get("VAULT_TOKEN"),
            VAULT_SECRETS_PATH3
        ));
  }

  @Test
  void shouldWorkWhenRegistryIsReloadedAndVaultIsRunning() {
    try (VaultContainer<?> vaultContainer2 = new VaultContainer<>(VAULT_IMAGE_NAME)) {
      vaultContainer2
          .withVaultToken(VAULT_TOKEN)
          .withVaultPort(8202)
          .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
          .waitingFor(VAULT_SERVER_STARTED)
          .start();
      String address = "http://" + vaultContainer2.getContainerIpAddress() + ':' + 8202;
      ConfigRegistrySettings settings =
          ConfigRegistrySettings.builder()
              .addLastSource(
                  "vault",
                  new VaultConfigSource(
                      address,
                      environmentVariables.get("VAULT_TOKEN"),
                      VAULT_SECRETS_PATH1
                  ))
              .reloadIntervalSec(1)
              .build();
      ConfigRegistry configRegistry = ConfigRegistry.create(settings);
      StringConfigProperty configProperty = configRegistry.stringProperty("top_secret");

      assertThat(configProperty.value().get(), containsString("password1"));
      try {
        ExecResult execResult =
            vaultContainer2.execInContainer(
                "/bin/sh", "-c", "vault write " + VAULT_SECRETS_PATH1 + " top_secret=new_password");
        assumeTrue(execResult.getStdout().contains("Success"));
        TimeUnit.SECONDS.sleep(2);
      } catch (Exception ignoredException) {
        fail("oops");
      }
      assertThat(configProperty.value().get(), containsString("new_password"));
    }
  }

  @Test
  void shouldWorkWhenRegistryIsReloadedAndVaultIsDown() {
    String PASSWORD_PROPERTY_NAME = "password";
    String PASSWORD_PROPERTY_VALUE = "123456";
    String secret = PASSWORD_PROPERTY_NAME + "=" + PASSWORD_PROPERTY_VALUE;
    try (VaultContainer<?> vaultContainer2 = new VaultContainer<>(VAULT_IMAGE_NAME)) {
      vaultContainer2
          .withVaultToken(VAULT_TOKEN)
          .withVaultPort(8203)
          .withEnv("VAULT_DEV_ROOT_TOKEN_ID", (String) VAULT_TOKEN)
          .withSecretInVault(VAULT_SECRETS_PATH1, secret)
          .waitingFor(VAULT_SERVER_STARTED)
          .start();

      String address = "http://" + vaultContainer2.getContainerIpAddress() + ':' + 8203;

      ConfigRegistrySettings settings =
          ConfigRegistrySettings.builder()
              .addLastSource(
                  "vault",
                  new VaultConfigSource(
                      address,
                      environmentVariables.get("VAULT_TOKEN"),
                      VAULT_SECRETS_PATH1
                  ))
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

      assertThat(configProperty.value().get(), containsString(PASSWORD_PROPERTY_VALUE));
    }
  }

  @Test
  void testSealed() throws Throwable {
    try (VaultContainer<?> vaultContainerSealed = new VaultContainer<>()) {
      vaultContainerSealed
          .withVaultToken(VAULT_TOKEN)
          .withVaultPort(8204)
          .waitingFor(VAULT_SERVER_STARTED)
          .start();

      String address = "http://" + vaultContainerSealed.getContainerIpAddress() + ':' + 8204;

      VaultTemplate vaultTemplate = new VaultTemplate(VaultEndpoint.from(new URI(address)),
          new TokenAuthentication(VAULT_TOKEN));

      vaultTemplate.opsForSys().seal();
      assumeTrue(vaultTemplate.opsForSys().getUnsealStatus().isSealed(), "vault seal status");

      Map<String, String> clientEnv = new HashMap<>();
      clientEnv.put("VAULT_TOKEN", "ROOT");
      clientEnv.put("VAULT_ADDR", address);
      clientEnv.put(VAULT_SECRETS_PATH, VAULT_SECRETS_PATH1);

      new VaultConfigSource(
          address,
          "ROOT",
          VAULT_SECRETS_PATH1
      ).loadConfig();
      fail("Negative test failed");
    } catch (ConfigSourceNotAvailableException expectedException) {
      assertThat(expectedException.getCause(), instanceOf(VaultException.class));
      String message = expectedException.getCause().getMessage();
      assertThat(message, containsString("Vault is sealed"));
    }
  }

  @Test
  void shouldWorkWhenRegistryIsReloadedAndVaultIsUnSealed()
      throws InterruptedException, URISyntaxException {
    AtomicReference<String> unsealKey = new AtomicReference<>();
    try (VaultContainer<?> sealedVaultContainer = new VaultContainer<>(VAULT_IMAGE_NAME)) {
      sealedVaultContainer
          .withVaultToken(VAULT_TOKEN)
          .withVaultPort(8205)
          .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
          .withLogConsumer(waitingForUnsealKey(unsealKey))
          .waitingFor(VAULT_SERVER_STARTED)
          .start();

      assumeTrue(unsealKey.get() != null, "unable to get unseal key");

      String address = "http://" + sealedVaultContainer.getContainerIpAddress() + ':' + 8205;

      ConfigRegistrySettings settings =
          ConfigRegistrySettings.builder()
              .addLastSource(
                  "vault",
                  new VaultConfigSource(
                      address,
                      VAULT_TOKEN,
                      VAULT_SECRETS_PATH1
                  ))
              .reloadIntervalSec(1)
              .build();

      ConfigRegistry configRegistry = ConfigRegistry.create(settings);
      StringConfigProperty configProperty = configRegistry.stringProperty("top_secret");

      assertThat(
          "initial value of top_secret", configProperty.value().get(), containsString("password1"));

      VaultTemplate vaultTemplate = new VaultTemplate(VaultEndpoint.from(new URI(address)),
          new TokenAuthentication(VAULT_TOKEN));

      Map<String, Object> newValues = new HashMap<>();
      newValues.put(configProperty.name(), "new_password");

      try {
        vaultTemplate.write(VAULT_SECRETS_PATH1, newValues);
        vaultTemplate.opsForSys().seal();
        assumeTrue(vaultTemplate.opsForSys().getUnsealStatus().isSealed(), "vault seal status");
      } catch (VaultException vaultException) {
        fail(vaultException.getMessage());
      }
      TimeUnit.SECONDS.sleep(2);
      assumeFalse(
          configProperty.value().isPresent()
              && configProperty.value().get().contains("new_password"),
          "new value was unexpectedly set");
      try {
        vaultTemplate.opsForSys().seal();
        vaultTemplate.opsForSys().unseal(unsealKey.get());
        assumeFalse(vaultTemplate.opsForSys().getUnsealStatus().isSealed(), "vault seal status");
      } catch (VaultException vaultException) {
        fail(vaultException.getMessage());
      }
      TimeUnit.SECONDS.sleep(2);
      assertThat(configProperty.value().get(), containsString("new_password"));
    }
  }
}
