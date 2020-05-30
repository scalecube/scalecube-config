package io.scalecube.config.vault;

import com.bettercloud.vault.VaultConfig;

@FunctionalInterface
public interface VaultTokenSupplier {

  String getToken(VaultConfig config);
}
