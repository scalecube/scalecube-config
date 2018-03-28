package io.scalecube.config.vault;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.source.LoadedConfigProperty.Builder;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VaultConfigSource implements ConfigSource {

  private Vault vault;
  private String SECRET_DEFAULT_PATH;

  VaultConfigSource(EnvironmentLoader environmentLoader) {
    this(Optional.of(new VaultConfig().environmentLoader(environmentLoader)));
    SECRET_DEFAULT_PATH = environmentLoader.loadVariable("VAULT_SECRETS_PATH");
  }

  public VaultConfigSource(Optional<VaultConfig> config) {
    SECRET_DEFAULT_PATH = System.getenv("VAULT_SECRETS_PATH");
    try {
      VaultConfig cfg = config.orElseGet(() -> new VaultConfig()
      // Defaults to "VAULT_ADDR" environment variable
      // .address("http://localhost:8200")
      // Defaults to "VAULT_TOKEN" environment variable
      // .token("00000000-0000-0000-0000-000000000000")
      // Defaults to "VAULT_OPEN_TIMEOUT" environment variable
      // .openTimeout(5)
      // Defaults to "VAULT_READ_TIMEOUT" environment variable
      // .readTimeout(30)
      // See "SSL Config" section below
      // .sslConfig(new SslConfig().build())
      );

      vault = new Vault(cfg.build());
      Boolean initialized = vault.debug().health().getInitialized();
      if (!initialized) {
        throw new VaultException("Vault yet initialized");
      }
      if (vault.seal().sealStatus().getSealed()) {
        throw new VaultException("Vault is sealed");
      }
    } catch (VaultException ignoredException) {
      ignoredException.printStackTrace();
    }
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    try {
      LogicalResponse response = vault.logical().read(SECRET_DEFAULT_PATH);

      return response.getData().entrySet().stream().map(LoadedConfigProperty::withNameAndValue).map(Builder::build)
          .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
    } catch (VaultException ignoredException) {
      return new HashMap<>();
    }
  }

}
