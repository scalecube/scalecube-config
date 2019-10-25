package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.VaultConfig;
import java.util.Objects;

public interface VaultTokenSupplier {

  default String getToken(EnvironmentLoader environmentLoader, VaultConfig config) {
    return Objects.requireNonNull(config.getToken(), "vault token");
  }
}
