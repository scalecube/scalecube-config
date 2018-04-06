package io.scalecube.config.vault;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import io.scalecube.config.utils.ThrowableUtil;

import com.bettercloud.vault.EnvironmentLoader;
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
 * @author AharonHa
 * @see <a href="https://www.vaultproject.io/">Vault Project</a>
 *
 */
public class VaultConfigSource implements ConfigSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfigSource.class);

  private Vault vault; // calculated

  private final String address;
  private final String token;
  private final String secretsPath;
  private final int connectTimeout;
  private final int readTimeout;

  /**
   * This constructor is used internally for test purposes. please use it only for tests
   *
   * @param environmentLoader an {@link EnvironmentLoader}
   */
  VaultConfigSource(EnvironmentLoader environmentLoader) {
    this(builder(environmentLoader.loadVariable("VAULT_ADDR"),
        environmentLoader.loadVariable("VAULT_TOKEN"),
        environmentLoader.loadVariable("VAULT_SECRETS_PATH")));
  }

  private VaultConfigSource(Builder builder) {
    this.address = builder.address;
    this.token = builder.token;
    this.secretsPath = builder.secretsPath;
    this.connectTimeout = builder.connectTimeout;
    this.readTimeout = builder.readTimeout;

    try {
      vault = new Vault(new VaultConfig()
          .address(address)
          .token(token)
          .openTimeout(connectTimeout)
          .readTimeout(readTimeout)
          .build());
    } catch (VaultException e) {
      LOGGER.error("Unable to build Vault config source", e);
      throw ThrowableUtil.propagate(e);
    }
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    try {
      LogicalResponse response = vault.logical().read(secretsPath);
      return response.getData().entrySet().stream()
          .map(LoadedConfigProperty::withNameAndValue)
          .map(LoadedConfigProperty.Builder::build)
          .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
    } catch (VaultException e) {
      LOGGER.error("Unable to read from Vault", e);
      throw new ConfigSourceNotAvailableException(e);
    }
  }

  public static Builder builder(String address, String token, String secretsPath) {
    return new Builder(address, token, secretsPath);
  }

  public static class Builder {

    private static final int DEFAULT_CONNECT_TIMEOUT = 3;
    private static final int DEFAULT_READ_TIMEOUT = 3;

    private final String address;
    private final String token;
    private final String secretsPath;
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    private Builder(String address, String token, String secretsPath) {
      this.address = address;
      this.token = token;
      this.secretsPath = secretsPath;
    }

    public Builder connectTimeout(int connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    public Builder readTimeout(int readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    public VaultConfigSource build() {
      return new VaultConfigSource(this);
    }
  }

}
