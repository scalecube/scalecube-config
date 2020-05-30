package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.response.LogicalResponse;
import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import io.scalecube.config.source.ConfigSource;
import io.scalecube.config.source.LoadedConfigProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private static final EnvironmentLoader ENVIRONMENT_LOADER = new EnvironmentLoader();

  private final VaultInvoker vault;
  private final List<String> secretsPaths;

  private VaultConfigSource(VaultInvoker vault, List<String> secretsPaths) {
    this.vault = vault;
    this.secretsPaths = new ArrayList<>(secretsPaths);
  }

  @Override
  public Map<String, ConfigProperty> loadConfig() {
    Map<String, ConfigProperty> result = new HashMap<>();
    for (String path : secretsPaths) {
      try {
        LogicalResponse response = vault.invoke(vault -> vault.logical().read(path));
        final Map<String, LoadedConfigProperty> pathProps =
            response.getData().entrySet().stream()
                .map(LoadedConfigProperty::withNameAndValue)
                .map(LoadedConfigProperty.Builder::build)
                .collect(Collectors.toMap(LoadedConfigProperty::name, Function.identity()));
        result.putAll(pathProps);
      } catch (Exception ex) {
        LOGGER.warn("Unable to load config properties from {}", path, ex);
        throw new ConfigSourceNotAvailableException(ex);
      }
    }
    return result;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {

    private Function<VaultInvoker.Builder, VaultInvoker.Builder> builderFunction = b -> b;

    private VaultInvoker invoker;

    private List<String> secretsPaths =
        Optional.ofNullable(ENVIRONMENT_LOADER.loadVariable("VAULT_SECRETS_PATH"))
            .or(() -> Optional.ofNullable(ENVIRONMENT_LOADER.loadVariable("VAULT_SECRETS_PATHS")))
            .map(s -> s.split(":"))
            .map(Arrays::asList)
            .orElseGet(ArrayList::new);

    private Builder() {}

    /**
     * @deprecated will be removed in future releases without notice, use {@link
     *     #addSecretsPath(String...)} or {@link #secretsPaths(Collection)}.
     */
    @Deprecated
    public Builder secretsPath(String secretsPath) {
      this.secretsPaths.addAll(toSecretsPaths(Collections.singletonList(secretsPath)));
      return this;
    }

    public Builder addSecretsPath(String... secretsPath) {
      this.secretsPaths.addAll(toSecretsPaths(Arrays.asList(secretsPath)));
      return this;
    }

    public Builder secretsPaths(Collection<String> secretsPaths) {
      this.secretsPaths = toSecretsPaths(secretsPaths);
      return this;
    }

    private static List<String> toSecretsPaths(Collection<String> secretsPaths) {
      return secretsPaths.stream()
          .flatMap(s -> Arrays.stream(s.split(":")))
          .distinct()
          .collect(Collectors.toList());
    }

    public Builder invoker(VaultInvoker invoker) {
      this.invoker = invoker;
      return this;
    }

    public Builder vault(UnaryOperator<VaultInvoker.Builder> config) {
      this.builderFunction = this.builderFunction.andThen(config);
      return this;
    }

    public Builder config(UnaryOperator<VaultConfig> vaultConfig) {
      this.builderFunction = this.builderFunction.andThen(c -> c.options(vaultConfig));
      return this;
    }

    public Builder tokenSupplier(VaultTokenSupplier supplier) {
      this.builderFunction = this.builderFunction.andThen(c -> c.tokenSupplier(supplier));
      return this;
    }

    /**
     * Builds vault config source.
     *
     * @return instance of {@link VaultConfigSource}
     */
    public VaultConfigSource build() {
      return new VaultConfigSource(
          invoker != null ? invoker : builderFunction.apply(new VaultInvoker.Builder()).build(),
          secretsPaths);
    }
  }
}
