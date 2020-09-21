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

  private String vaultRole = ENVIRONMENT_LOADER.loadVariable("VAULT_ROLE");

  private String vaultJwtProvider =
      Optional.ofNullable(ENVIRONMENT_LOADER.loadVariable("VAULT_JWT_PROVIDER"))
          .orElse("kubernetes");

  private String serviceAccountTokenPath =
      Optional.ofNullable(ENVIRONMENT_LOADER.loadVariable("SERVICE_ACCOUNT_TOKEN_PATH"))
          .orElse("/var/run/secrets/kubernetes.io/serviceaccount/token");

  public KubernetesVaultTokenSupplier vaultRole(String vaultRole) {
    this.vaultRole = vaultRole;
    return this;
  }

  public KubernetesVaultTokenSupplier vaultJwtProvider(String vaultJwtProvider) {
    this.vaultJwtProvider = vaultJwtProvider;
    return this;
  }

  public KubernetesVaultTokenSupplier serviceAccountTokenPath(String serviceAccountTokenPath) {
    this.serviceAccountTokenPath = serviceAccountTokenPath;
    return this;
  }

  @Override
  public String getToken(VaultConfig config) {
    Objects.requireNonNull(vaultRole, "vault role");
    Objects.requireNonNull(vaultJwtProvider, "jwt provider");
    Objects.requireNonNull(serviceAccountTokenPath, "k8s service account token path");
    try (Stream<String> stream = Files.lines(Paths.get(serviceAccountTokenPath))) {
      String jwt = stream.collect(Collectors.joining());
      return Objects.requireNonNull(
          new Vault(config)
              .auth()
              .loginByJwt(vaultJwtProvider, vaultRole, jwt)
              .getAuthClientToken(),
          "vault token");
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }
}
