package io.scalecube.config.vault;

import static java.util.Objects.requireNonNull;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is a {@link ConfigSource} implemented for Vault
 * 
 * @see <a href="https://www.vaultproject.io/">Vault Project</a>
 */
public class VaultConfigSource implements ConfigSource {

  static final Logger LOGGER = LoggerFactory.getLogger(VaultConfigSource.class);

  private final Vault vault;
  private final String secretsPath;

  /**
   * Create a new {@link VaultConfigSource} with the given {@link Builder}. <br>
   * 
   * @param builder configuration to create vault access with.
   * 
   */
  VaultConfigSource(Builder builder) {
    this.secretsPath = builder.secretsPath();
    vault = new Vault(builder.config);
  }

  private void checkVaultStatus() throws VaultException {
    if (vault.seal().sealStatus().getSealed()) {
      throw new VaultException("Vault is sealed");
    }
    Boolean initialized = vault.debug().health().getInitialized();
    if (!initialized) {
      throw new VaultException("Vault not yet initialized");
    }
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    try {
      checkVaultStatus();
      LogicalResponse response = vault.logical().read(this.secretsPath);
      return response.getData().entrySet().stream().map(LoadedConfigProperty::withNameAndValue)
          .map(LoadedConfigProperty.Builder::build)
          .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
    } catch (VaultException vaultException) {
      LOGGER.warn("unable to load config properties", vaultException);
      throw new ConfigSourceNotAvailableException(vaultException);
    }
  }

  /**
   * This builder method is used internally for test purposes. please use it only for tests. Please note the following
   * required environment variables are required.
   * <ul>
   * <li><code>VAULT_SECRETS_PATH</pre> is the path to use (defaults to <code>secret</code>)</li>
   * <li><code>VAULT_TOKEN</code> is the {@link VaultConfig#token(String) token} to use</li>
   * <li><code>VAULT_ADDR</code> is the {@link VaultConfig#address(String) address} of the vault (API)</li>
   * </ul>
   */
  public static Builder builder() {
    return builder(new EnvironmentLoader());
  }

  /**
   * This builder method is used internally for test purposes. please use it only for tests
   *
   * @param environmentLoader an {@link EnvironmentLoader}
   */
  static Builder builder(EnvironmentLoader environmentLoader) {
    return builder(environmentLoader.loadVariable("VAULT_ADDR"),
        environmentLoader.loadVariable("VAULT_TOKEN"),
        environmentLoader.loadVariable("VAULT_SECRETS_PATH"));
  }

  public static Builder builder(String address, String token, String secretsPath) {
    return new Builder(address, token, secretsPath);
  }

  public static final class Builder {

    final VaultConfig config = new VaultConfig();
    private final String secretsPath;

    Builder(String address, String token, String secretsPath) {
      config.address(requireNonNull(address, "Missing address"))
          .token(requireNonNull(token, "Missing token"))
          .sslConfig(new SslConfig());
      this.secretsPath = requireNonNull(secretsPath, "Missing secretsPath");
    }

    public Builder connectTimeout(int connectTimeout) {
      config.openTimeout(connectTimeout);
      return this;
    }

    public Builder readTimeout(int readTimeout) {
      config.readTimeout(readTimeout);
      return this;
    }

    public VaultConfigSource build() {
      try {
        this.config.build();
        return new VaultConfigSource(this);
      } catch (VaultException propogateException) {
        LOGGER.error("Unable to build " + VaultConfigSource.class.getSimpleName(), propogateException);
        throw ThrowableUtil.propagate(propogateException);
      }
    }

    public String secretsPath() {
      return secretsPath;
    }
  }
}
