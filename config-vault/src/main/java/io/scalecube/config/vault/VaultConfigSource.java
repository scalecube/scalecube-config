package io.scalecube.config.vault;

import static java.util.Objects.requireNonNull;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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

  private final Vault vault;
  private final String secretsPath;
  private final Duration renewEvery;

  /**
   * Create a new {@link VaultConfigSource} with the given {@link Builder}.
   *
   * @param builder configuration to create vault access with.
   */
  private VaultConfigSource(Builder builder) {
    this.secretsPath = builder.secretsPath();
    this.renewEvery = builder.renewEvery;
    vault = new Vault(builder.config);

    if (renewEvery != null) {
      long initialDelay = renewEvery.toMillis();
      long period = renewEvery.toMillis();
      TimeUnit unit = TimeUnit.MILLISECONDS;
      ThreadFactory factory =
          r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName(VaultConfigSource.class.getSimpleName() + " token renewer");
            return thread;
          };
      Executors.newScheduledThreadPool(1, factory)
          .scheduleAtFixedRate(
              () -> {
                try {
                  vault.auth().renewSelf();
                  LOGGER.info("renew token success");
                } catch (VaultException vaultException) {
                  LOGGER.error("failed to renew token", vaultException);
                }
              },
              initialDelay,
              period,
              unit);
    }
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
    return builder(new EnvironmentLoader());
  }

  /**
   * This builder method is used internally for test purposes. please use it only for tests
   *
   * @param environmentLoader an {@link EnvironmentLoader}
   */
  static Builder builder(EnvironmentLoader environmentLoader) {
    return builder(
        environmentLoader.loadVariable("VAULT_ADDR"),
        environmentLoader.loadVariable("VAULT_TOKEN"),
        environmentLoader.loadVariable("VAULT_SECRETS_PATH"));
  }

  public static Builder builder(String address, String token, String secretsPath) {
    return new Builder(address, token, secretsPath);
  }

  public static final class Builder {

    final VaultConfig config = new VaultConfig();
    private final String secretsPath;
    Duration renewEvery = null;

    Builder(String address, String token, String secretsPath) {
      config
          .address(requireNonNull(address, "Missing address"))
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

    public Builder renewEvery(Duration duration) {
      renewEvery = duration;
      return this;
    }

    /**
     * Builds vault config source.
     *
     * @return instance of {@link VaultConfigSource}
     */
    public VaultConfigSource build() {
      try {
        this.config.build();
        return new VaultConfigSource(this);
      } catch (VaultException propogateException) {
        LOGGER.error(
            "Unable to build " + VaultConfigSource.class.getSimpleName(), propogateException);
        throw ThrowableUtil.propagate(propogateException);
      }
    }

    public String secretsPath() {
      return secretsPath;
    }
  }
}
