package io.scalecube.config.vault;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Objects;

public class VaultInvokers {

  public static final Logger LOGGER = System.getLogger(VaultInvokers.class.getName());

  public static final String VAULT_MOUNT_POINT_ENV = "VAULT_MOUNT_POINT";
  public static final String VAULT_ADDR_ENV = "VAULT_ADDR";
  public static final String VAULT_TOKEN_ENV = "VAULT_TOKEN";
  public static final String VAULT_ROLE_ENV = "VAULT_ROLE";
  public static final String VAULT_JWT_PROVIDER_ENV = "VAULT_JWT_PROVIDER";
  public static final String VAULT_ENGINE_VERSION_ENV = "VAULT_ENGINE_VERSION";

  public static final String DEFAULT_VAULT_ENGINE_VERSION = "1";

  private VaultInvokers() {
    // Do not instantiate
  }

  /**
   * Creates {@link VaultInvoker}, or throws error if {@link VaultInvoker} instance cannot be
   * created.
   *
   * @return new {@code VaultInvoker} instance, or throws error
   */
  public static VaultInvoker newVaultInvokerOrThrow() {
    final VaultInvoker vaultInvoker = newVaultInvoker();
    if (vaultInvoker == null) {
      throw new IllegalStateException("Cannot create vaultInvoker");
    }
    return vaultInvoker;
  }

  /**
   * Creates and returns new {@link VaultInvoker} instance.
   *
   * @return new {@code VaultInvoker} instance
   */
  public static VaultInvoker newVaultInvoker() {
    Map<String, String> env = System.getenv();

    final String vaultAddr = env.get(VAULT_ADDR_ENV);
    final int vaultEngineVersion =
        Integer.parseInt(env.getOrDefault(VAULT_ENGINE_VERSION_ENV, DEFAULT_VAULT_ENGINE_VERSION));

    if (isNullOrNone(vaultAddr)) {
      return null;
    }

    String vaultToken = env.get(VAULT_TOKEN_ENV);
    String vaultRole = env.get(VAULT_ROLE_ENV);

    if (isNullOrNone(vaultToken) && isNullOrNone(vaultRole)) {
      throw new IllegalArgumentException(
          "Vault auth scheme is required (specify either VAULT_ROLE or VAULT_TOKEN)");
    }

    final VaultInvoker.Builder builder =
        new VaultInvoker.Builder()
            .options(config -> config.address(vaultAddr).engineVersion(vaultEngineVersion));

    if (!isNullOrNone(vaultRole)) {
      if (!isNullOrNone(vaultToken)) {
        LOGGER.log(
            Level.WARNING,
            "Taking KubernetesVaultTokenSupplier by precedence rule, "
                + "ignoring EnvironmentVaultTokenSupplier "
                + "(specify either VAULT_ROLE or VAULT_TOKEN, not both)");
      }
      builder.tokenSupplier(new KubernetesVaultTokenSupplier.Builder().build());
    } else {
      builder.tokenSupplier(new EnvironmentVaultTokenSupplier());
    }

    return builder.build();
  }

  private static boolean isNullOrNone(String value) {
    return Objects.isNull(value) || "none".equalsIgnoreCase(value);
  }
}
