package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import io.scalecube.config.utils.ThrowableUtil;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KubernetesVaultTokenSupplier implements VaultTokenSupplier {

  private static final EnvironmentLoader ENVIRONMENT_LOADER = new EnvironmentLoader();

  private final String vaultRole;
  private final String vaultJwtProvider;
  private final String serviceAccountTokenPath;

  private KubernetesVaultTokenSupplier(Builder builder) {
    this.vaultRole = Objects.requireNonNull(builder.vaultRole, "vault role");
    this.vaultJwtProvider = Objects.requireNonNull(builder.vaultJwtProvider, "jwt provider");
    this.serviceAccountTokenPath =
        Objects.requireNonNull(builder.serviceAccountTokenPath, "k8s service account token path");
  }

  public static Builder builder() {
    return new Builder();
  }

  public static KubernetesVaultTokenSupplier newInstance() {
    return builder().build();
  }

  @Override
  public String getToken(VaultConfig config) {
    try (Stream<String> stream = Files.lines(Paths.get(serviceAccountTokenPath))) {
      String jwt = stream.collect(Collectors.joining());
      return new Vault(config)
          .auth()
          .loginByJwt(vaultJwtProvider, vaultRole, jwt)
          .getAuthClientToken();
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }

  public static class Builder {

    private String vaultRole = ENVIRONMENT_LOADER.loadVariable("VAULT_ROLE");

    private String vaultJwtProvider =
        Optional.ofNullable(
                Optional.ofNullable(ENVIRONMENT_LOADER.loadVariable("VAULT_JWT_PROVIDER"))
                    .orElse(ENVIRONMENT_LOADER.loadVariable("VAULT_MOUNT_POINT")))
            .orElse("kubernetes");

    private String serviceAccountTokenPath =
        Optional.ofNullable(ENVIRONMENT_LOADER.loadVariable("SERVICE_ACCOUNT_TOKEN_PATH"))
            .orElse("/var/run/secrets/kubernetes.io/serviceaccount/token");

    private Builder() {}

    public Builder vaultRole(String vaultRole) {
      this.vaultRole = vaultRole;
      return this;
    }

    public Builder vaultJwtProvider(String vaultJwtProvider) {
      this.vaultJwtProvider = vaultJwtProvider;
      return this;
    }

    public Builder serviceAccountTokenPath(String serviceAccountTokenPath) {
      this.serviceAccountTokenPath = serviceAccountTokenPath;
      return this;
    }

    public KubernetesVaultTokenSupplier build() {
      return new KubernetesVaultTokenSupplier(this);
    }
  }
}
