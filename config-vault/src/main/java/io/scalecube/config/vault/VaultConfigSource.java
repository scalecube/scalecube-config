package io.scalecube.config.vault;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.vault.VaultException;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
import org.springframework.web.client.ResourceAccessException;

/**
 * This class is a {@link ConfigSource} implemented for Vault.
 *
 * @see <a href="https://www.vaultproject.io/">Vault Project</a>
 */
public class VaultConfigSource implements ConfigSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfigSource.class);

  private final String secretsPath;
  private final VaultTemplate vaultTemplate;

  /**
   * Vault configuration source constructor based on all necessary credentials.
   *
   * @param address address of Vault service provider
   * @param token token to access
   * @param secretsPath secret path
   */
  public VaultConfigSource(String address, String token, String secretsPath) {
    this.secretsPath = secretsPath;

    try {
      this.vaultTemplate = new VaultTemplate(VaultEndpoint.from(new URI(address)),
          new TokenAuthentication(token));
    } catch (URISyntaxException | IllegalArgumentException e) {
      LOGGER.warn("Can't access by URI", e);
      throw new ConfigSourceNotAvailableException(e);
    }
  }

  private void checkVaultStatus() throws VaultException {
    if (vaultTemplate.opsForSys().getUnsealStatus().isSealed()) {
      throw new VaultException("Vault is sealed");
    }
    Boolean initialized = vaultTemplate.opsForSys().health().isInitialized();
    if (!initialized) {
      throw new VaultException("Vault not yet initialized");
    }
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    try {
      checkVaultStatus();
      Optional<VaultResponse> response = Optional.ofNullable(vaultTemplate.read(this.secretsPath));
      return response.orElseThrow(() -> new VaultException("Seems path doesn't exists"))
          .getData()
          .entrySet()
          .stream()
          .collect(Collectors.toMap(
              e -> e.getKey(),
              e -> String.valueOf(e.getValue())
          ))
          .entrySet()
          .stream()
          .map(LoadedConfigProperty::withNameAndValue)
          .map(LoadedConfigProperty.Builder::build)
          .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
    } catch (VaultException | ResourceAccessException vaultException) {
      LOGGER.warn("unable to load config properties", vaultException);
      throw new ConfigSourceNotAvailableException(vaultException);
    }
  }
}
