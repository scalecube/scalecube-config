package io.scalecube.config.vault;

import static io.scalecube.config.vault.VaultInvoker.STATUS_CODE_NOT_FOUND;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an implementation of {@link ConfigSource} for Vault.
 *
 * @see <a href="https://www.vaultproject.io/">Vault Project</a>
 */
public class VaultConfigSource implements ConfigSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfigSource.class);

  private static final EnvironmentLoader ENVIRONMENT_LOADER = new EnvironmentLoader();
  private static final String PATHS_SEPARATOR = ":";

  private final VaultInvoker vault;
  private final Collection<String> secretsPaths;

  private VaultConfigSource(VaultInvoker vault, Collection<String> secretsPaths) {
    this.vault = vault;
    this.secretsPaths = new ArrayList<>(secretsPaths);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    Map<String, ConfigProperty> propertyMap = new HashMap<>();
    for (String path : secretsPaths) {
      try {
        LogicalResponse response = vault.invoke(vault -> vault.logical().read(path));
        final Map<String, LoadedConfigProperty> pathProps =
            response.getData().entrySet().stream()
                .map(LoadedConfigProperty::withNameAndValue)
                .map(LoadedConfigProperty.Builder::build)
                .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
        propertyMap.putAll(pathProps);
      } catch (VaultException ex) {
        if (ex.getHttpStatusCode() == STATUS_CODE_NOT_FOUND) {
          LOGGER.error("Unable to load config properties from: {}", path);
        } else {
          throw new ConfigSourceNotAvailableException(ex);
        }
      } catch (Exception ex) {
        LOGGER.error("Unable to load config properties from: {}", path, ex);
        throw new ConfigSourceNotAvailableException(ex);
      }
    }
    return propertyMap;
  }

  public static final class Builder {

    private Function<VaultInvoker.Builder, VaultInvoker.Builder> builderFunction = b -> b;

    private VaultInvoker invoker;

    private Set<String> secretsPaths =
        Optional.ofNullable(
                Optional.ofNullable(ENVIRONMENT_LOADER.loadVariable("VAULT_SECRETS_PATH"))
                    .orElse(ENVIRONMENT_LOADER.loadVariable("VAULT_SECRETS_PATHS")))
            .map(s -> s.split(PATHS_SEPARATOR))
            .map(Arrays::asList)
            .map(HashSet::new)
            .orElseGet(HashSet::new);

    private Builder() {}

    /**
     * Appends secrets paths (each path value may contain values separated by colons).
     *
     * @param secretsPath secretsPath
     * @return this
     */
    public Builder addSecretsPath(String... secretsPath) {
      secretsPaths.addAll(toSecretsPaths(Arrays.asList(secretsPath)));
      return this;
    }

    /**
     * Setter for secrets paths (each path value may contain values separated by colons).
     *
     * @param secretsPaths secretsPaths
     * @return this
     */
    public Builder secretsPaths(Collection<String> secretsPaths) {
      this.secretsPaths = toSecretsPaths(secretsPaths);
      return this;
    }

    private static Set<String> toSecretsPaths(Collection<String> secretsPaths) {
      return secretsPaths.stream()
          .flatMap(s -> Arrays.stream(s.split(PATHS_SEPARATOR)))
          .collect(Collectors.toSet());
    }

    /**
     * Setter for {@link VaultInvoker}.
     *
     * @param vaultInvoker vaultInvoker
     * @return this
     */
    public Builder invoker(VaultInvoker vaultInvoker) {
      this.invoker = vaultInvoker;
      return this;
    }

    /**
     * Setter for {@link VaultInvoker.Builder} operator.
     *
     * @param operator operator for {@link VaultInvoker.Builder}
     * @return this
     */
    public Builder vault(UnaryOperator<VaultInvoker.Builder> operator) {
      this.builderFunction = this.builderFunction.andThen(operator);
      return this;
    }

    /**
     * Setter for {@link VaultConfig}.
     *
     * @param vaultConfig vaultConfig
     * @return this
     */
    public Builder config(UnaryOperator<VaultConfig> vaultConfig) {
      this.builderFunction = this.builderFunction.andThen(b -> b.options(vaultConfig));
      return this;
    }

    /**
     * Setter for {@link VaultTokenSupplier}.
     *
     * @param tokenSupplier tokenSupplier
     * @return this
     */
    public Builder tokenSupplier(VaultTokenSupplier tokenSupplier) {
      this.builderFunction = this.builderFunction.andThen(b -> b.tokenSupplier(tokenSupplier));
      return this;
    }

    public VaultConfigSource build() {
      return new VaultConfigSource(
          invoker != null ? invoker : builderFunction.apply(VaultInvoker.builder()).build(),
          secretsPaths);
    }
  }
}
