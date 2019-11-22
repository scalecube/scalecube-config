package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
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

  private final VaultInvoker vault;
  private final String secretsPath;

  /**
   * Create a new {@link VaultConfigSource}.
   *
   * @param vault vault invoker.
   * @param secretsPath secret path.
   */
  private VaultConfigSource(VaultInvoker vault, String secretsPath) {
    this.vault = vault;
    this.secretsPath = secretsPath;
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    try {
      LogicalResponse response = vault.invoke(vault -> vault.logical().read(secretsPath));
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
    return new Builder();
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

    private Function<VaultInvoker.Builder, VaultInvoker.Builder> vault = Function.identity();
    private VaultInvoker invoker;
    private EnvironmentLoader environmentLoader = VaultInvoker.Builder.ENVIRONMENT_LOADER;
    private String secretsPath;

    private Builder() {}

    public Builder secretsPath(String secretsPath) {
      this.secretsPath = secretsPath;
      return this;
    }

    public Builder invoker(VaultInvoker invoker) {
      this.invoker = invoker;
      return this;
    }

    public Builder vault(UnaryOperator<VaultInvoker.Builder> config) {
      this.vault = this.vault.andThen(config);
      return this;
    }

    public Builder config(UnaryOperator<VaultConfig> vaultConfig) {
      this.vault = this.vault.andThen(c -> c.options(vaultConfig));
      return this;
    }

    public Builder tokenSupplier(VaultTokenSupplier supplier) {
      this.vault = this.vault.andThen(c -> c.tokenSupplier(supplier));
      return this;
    }

    /**
     * Builds vault config source.
     *
     * @return instance of {@link VaultConfigSource}
     */
    public VaultConfigSource build() {
      VaultInvoker vaultInvoker =
          invoker != null
              ? invoker
              : vault.apply(new VaultInvoker.Builder(environmentLoader)).build();
      secretsPath =
          Objects.requireNonNull(
              secretsPath != null
                  ? secretsPath
                  : environmentLoader.loadVariable(VAULT_SECRETS_PATH),
              "Missing secretsPath");
      return new VaultConfigSource(vaultInvoker, secretsPath);
    }
  }
}
