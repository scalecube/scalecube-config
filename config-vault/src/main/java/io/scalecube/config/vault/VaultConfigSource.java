package io.scalecube.config.vault;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.source.LoadedConfigProperty.Builder;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is a {@link ConfigSource} implemented for Vault
 * 
 * @author AharonHa
 * @see The Vault Project's site at {@link https://www.vaultproject.io/}
 *
 */
public class VaultConfigSource implements ConfigSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfigSource.class);
  private Vault vault;
  private String SECRET_DEFAULT_PATH;

  /**
   * This constructor is used internally for test purposes. please use it only for tests
   * 
   * @param environmentLoader an {@link EnvironmentLoader}
   */
  VaultConfigSource(EnvironmentLoader environmentLoader) {
    this(Optional.of(new VaultConfig().environmentLoader(environmentLoader)));
    SECRET_DEFAULT_PATH = environmentLoader.loadVariable("VAULT_SECRETS_PATH");
  }

  public VaultConfigSource() {
    this(Optional.empty());
  }

  /**
   * Create a new {@link VaultConfigSource} with the given {@link VaultConfig}. <br>
   * Default configurations can also be used by passing {@link Optional#empty() empty}. Please note the following
   * required environment variables are required if the configuration does not provide them
   * <ul>
   * <li><code>VAULT_SECRETS_PATH</pre> is the path to use (defaults to <code>secret</code>)</li>
   * <li><code>VAULT_TOKEN</code> is the {@link VaultConfig#token(String) token} to use</li>
   * <li><code>VAULT_ADDR</code> is the {@link VaultConfig#address(String) address} of the vault (API)</li>
   * </ul>
   * 
   * @param config an optional configuration to create vault access with.
   * 
   */

  public VaultConfigSource(Optional<VaultConfig> config) {
    SECRET_DEFAULT_PATH = System.getenv().getOrDefault("VAULT_SECRETS_PATH", "secret");
    try {
      VaultConfig cfg = config.orElseGet(() -> new VaultConfig());
      if (cfg.build().getToken() == null) {
        throw new VaultException("Missing Vault token");
      }
      vault = new Vault(cfg.build());
      checkVaultHealth();
    } catch (VaultException exception) {
      LOGGER.error("unable to build vault config source", exception);
      vault = null;
    }
  }

  private void checkVaultHealth() throws VaultException {
    if (vault != null) {
      Boolean initialized = vault.debug().health().getInitialized();
      if (!initialized) {
        throw new VaultException("Vault not yet initialized");
      }
      if (vault.seal().sealStatus().getSealed()) {
        throw new VaultException("Vault is sealed");
      }
    } else {
      throw new VaultException("Vault instance is unhealthy");
    }
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    try {
      checkVaultHealth();
    } catch (VaultException ignoredException) {
      LOGGER.warn("unable to read from vault", ignoredException);
      throw new ConfigSourceNotAvailableException("unable to read from vault", ignoredException);
    }
    try {
      LogicalResponse response = vault.logical().read(SECRET_DEFAULT_PATH);
      return response.getData().entrySet().stream().map(LoadedConfigProperty::withNameAndValue).map(Builder::build)
          .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
    } catch (VaultException ignoredException) {
      return Collections.emptyMap();
    }
  }

}
