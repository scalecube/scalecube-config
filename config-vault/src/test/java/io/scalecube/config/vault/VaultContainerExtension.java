package io.scalecube.config.vault;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.vault.VaultContainer;

public class VaultContainerExtension implements AfterAllCallback, BeforeAllCallback {

  static final String VAULT_IMAGE_NAME = "vault:0.9.6";
  static final int VAULT_PORT = 8200;
  static final String VAULT_TOKEN = "my-root-token";
  /**
   * the environment variable name for vault secret path
   */
  static final String VAULT_SECRETS_PATH = "VAULT_SECRETS_PATH";

  // these 3 are actual values we would like to test with
  static final String VAULT_SECRETS_PATH1 = "secret/application/tenant1";
  static final String VAULT_SECRETS_PATH2 = "secret/application/tenant2";
  static final String VAULT_SECRETS_PATH3 = "secret/application2/tenant3";

  static final WaitStrategy VAULT_SERVER_STARTED = new LogMessageWaitStrategy()
      .withRegEx("==> Vault server started! Log data will stream in below:\n")
      .withTimes(1);

  private VaultContainer vaultContainer;

  @Override
  public void afterAll(ExtensionContext context) {
    if (vaultContainer.isRunning()) {
      vaultContainer.stop();
    }
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    vaultContainer = new VaultContainer<>()
        .waitingFor(VAULT_SERVER_STARTED)
        .withVaultToken(VAULT_TOKEN)
        .withVaultPort(VAULT_PORT)
        .withSecretInVault(VAULT_SECRETS_PATH1, "top_secret=password1", "db_password=dbpassword1")
        .withSecretInVault(VAULT_SECRETS_PATH2, "top_secret=password2", "db_password=dbpassword2")
        .withSecretInVault(VAULT_SECRETS_PATH3, "secret=password", "password=dbpassword");
    vaultContainer.start();
  }

  VaultContainer container() {
    return vaultContainer;
  }

}
