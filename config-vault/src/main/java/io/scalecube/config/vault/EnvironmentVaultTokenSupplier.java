package io.scalecube.config.vault;

import com.bettercloud.vault.VaultConfig;
import java.util.Objects;

public class EnvironmentVaultTokenSupplier implements VaultTokenSupplier {

  public String getToken(VaultConfig config) {
    return Objects.requireNonNull(config.getToken(), "VaultConfig.token is missing");
  }
}
