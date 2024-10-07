package io.scalecube.config.vault;

import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class VaultClientTokenSupplier {

  private static final Logger LOGGER = System.getLogger(VaultClientTokenSupplier.class.getName());

  private final String vaultAddress;
  private final String vaultToken;
  private final String vaultRole;

  /**
   * Constructor.
   *
   * @param vaultAddress vaultAddress
   * @param vaultToken vaultToken (must not set be together with vaultRole)
   * @param vaultRole vaultRole (must not set be together with vaultToken)
   */
  public VaultClientTokenSupplier(String vaultAddress, String vaultToken, String vaultRole) {
    this.vaultAddress = vaultAddress;
    this.vaultToken = vaultToken;
    this.vaultRole = vaultRole;
    if (isNullOrNoneOrEmpty(vaultAddress)) {
      throw new IllegalArgumentException("Vault address is required");
    }
    if (isNullOrNoneOrEmpty(vaultToken) && isNullOrNoneOrEmpty(vaultRole)) {
      throw new IllegalArgumentException(
          "Vault auth scheme is required (specify either vaultToken or vaultRole)");
    }
  }

  /**
   * Returns new instance of {@link VaultClientTokenSupplier}.
   *
   * @param vaultAddress vaultAddress
   * @param vaultToken vaultToken
   * @return new instance of {@link VaultClientTokenSupplier}
   */
  public static VaultClientTokenSupplier supplierByToken(String vaultAddress, String vaultToken) {
    return new VaultClientTokenSupplier(vaultAddress, vaultToken, null);
  }

  /**
   * Returns new instance of {@link VaultClientTokenSupplier}.
   *
   * @param vaultAddress vaultAddress
   * @param vaultRole vaultRole
   * @return new instance of {@link VaultClientTokenSupplier}
   */
  public static VaultClientTokenSupplier supplierByRole(String vaultAddress, String vaultRole) {
    return new VaultClientTokenSupplier(vaultAddress, null, vaultRole);
  }

  /**
   * Obtains vault client token.
   *
   * @return future result
   */
  public Future<String> getToken() {
    return CompletableFuture.supplyAsync(this::getToken0);
  }

  private String getToken0() {
    try {
      VaultTokenSupplier vaultTokenSupplier;
      VaultConfig vaultConfig;

      if (!isNullOrNoneOrEmpty(vaultRole)) {
        if (!isNullOrNoneOrEmpty(vaultToken)) {
          LOGGER.log(
              Level.WARNING,
              "Taking KubernetesVaultTokenSupplier by precedence rule, "
                  + "ignoring EnvironmentVaultTokenSupplier "
                  + "(specify either vaultToken or vaultRole, not both)");
        }
        vaultTokenSupplier =
            new KubernetesVaultTokenSupplier.Builder().vaultRole(vaultRole).build();
        vaultConfig = new VaultConfig().address(vaultAddress).build();
      } else {
        vaultTokenSupplier = new EnvironmentVaultTokenSupplier();
        vaultConfig = new VaultConfig().address(vaultAddress).token(vaultToken).build();
      }

      return vaultTokenSupplier.getToken(vaultConfig);
    } catch (VaultException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isNullOrNoneOrEmpty(String value) {
    return Objects.isNull(value)
        || "none".equalsIgnoreCase(value)
        || "null".equals(value)
        || value.isEmpty();
  }
}
