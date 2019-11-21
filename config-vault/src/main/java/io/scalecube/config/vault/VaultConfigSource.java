package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a {@link ConfigSource} implemented for Vault.
 *
 * @see <a href="https://www.vaultproject.io/">Vault Project</a>
 */
public class VaultConfigSource implements ConfigSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfigSource.class);

  private static final String VAULT_SECRETS_PATH = "VAULT_SECRETS_PATH";

  private final VaultSupplier vaultSupplier;
  private final String secretsPath;

  /**
   * Create a new {@link VaultConfigSource} with the given {@link Builder}.
   *
   * @param builder configuration to create vault access with.
   */
  private VaultConfigSource(Builder builder) {
    vaultSupplier =
        new VaultSupplier(builder.config, builder.tokenSupplier, builder.environmentLoader);
    secretsPath =
        Objects.requireNonNull(
            builder.secretsPath != null
                ? builder.secretsPath
                : builder.environmentLoader.loadVariable(VAULT_SECRETS_PATH),
            "Missing secretsPath");
  }

  private void checkVaultStatus(Vault vault) throws VaultException {
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
      Vault vault = vaultSupplier.get();
      checkVaultStatus(vault);
      LogicalResponse response = vault.logical().read(secretsPath);
      return response.getData().entrySet().stream()
          .map(LoadedConfigProperty::withNameAndValue)
          .map(LoadedConfigProperty.Builder::build)
          .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
    } catch (VaultException vaultException) {
      LOGGER.warn("unable to load config properties", vaultException);
      throw new ConfigSourceNotAvailableException(vaultException);
    }
  }

  /**
   * This builder method is used internally for test purposes. please use it only for tests. Please
   * note the following required environment variables are required.
   *
   * <ul>
   *   <li><code>VAULT_SECRETS_PATH</code> is the path to use (defaults to <code>secret</code>)
   *   <li><code>VAULT_TOKEN</code> is the {@link VaultConfig#token(String) token} to use
   *   <li><code>VAULT_ADDR</code> is the {@link VaultConfig#address(String) address} of the vault
   *       (API)
   * </ul>
   */
  public static Builder builder() {
    return builder(Builder.ENVIRONMENT_LOADER);
  }

  /**
   * This builder method is used internally for test purposes. please use it only for tests
   *
   * @param environmentLoader an {@link EnvironmentLoader}
   */
  static Builder builder(EnvironmentLoader environmentLoader) {
    final Builder builder = new Builder();
    if (environmentLoader != null) {
      builder.environmentLoader = environmentLoader;
    }
    return builder;
  }

  public static final class Builder {

    private static final EnvironmentLoader ENVIRONMENT_LOADER = new EnvironmentLoader();

    private Function<VaultConfig, VaultConfig> config = Function.identity();
    private VaultTokenSupplier tokenSupplier = new VaultTokenSupplier() {};
    private EnvironmentLoader environmentLoader = ENVIRONMENT_LOADER;
    private String secretsPath;

    private Builder() {}

    public Builder secretsPath(String secretsPath) {
      this.secretsPath = secretsPath;
      return this;
    }

    public Builder config(UnaryOperator<VaultConfig> config) {
      this.config = this.config.andThen(config);
      return this;
    }

    public Builder tokenSupplier(VaultTokenSupplier supplier) {
      this.tokenSupplier = supplier;
      return this;
    }

    /**
     * Builds vault config source.
     *
     * @return instance of {@link VaultConfigSource}
     */
    public VaultConfigSource build() {
      return new VaultConfigSource(this);
    }
  }
}
