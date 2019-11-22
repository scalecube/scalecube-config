package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LookupResponse;
import com.bettercloud.vault.response.VaultResponse;
import com.bettercloud.vault.rest.RestResponse;
import io.scalecube.config.ConfigSourceNotAvailableException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultInvoker {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultInvoker.class);

  private static final long MIN_REFRESH_MARGIN = TimeUnit.MINUTES.toSeconds(10);

  private final Builder config;

  private Vault vault;
  private Timer timer;

  public static Builder builder() {
    return new Builder(Builder.ENVIRONMENT_LOADER);
  }

  private VaultInvoker(Builder config) {
    this.config = config;
  }

  /**
   * Invokes a given call with vault.
   *
   * @param call call
   * @return vault response
   */
  public <T extends VaultResponse> T invoke(VaultCall<T> call) throws VaultException {
    Vault vault = this.vault;
    try {
      if (vault == null) {
        vault = recreateVault(null);
      }
      T response = call.apply(vault);
      checkResponse(response.getRestResponse());
      return response;
    } catch (VaultException e) {
      LOGGER.warn("Exception occurred during invoking Vault", e);
      // try recreate Vault according to https://www.vaultproject.io/api/overview#http-status-codes
      if (e.getHttpStatusCode() == 403) {
        vault = recreateVault(vault);
        return call.apply(vault);
      }
      throw e;
    }
  }

  private synchronized Vault recreateVault(Vault prev) {
    try {
      if (!Objects.equals(prev, vault) && vault != null) {
        return vault;
      }
      if (timer != null) {
        timer.cancel();
        timer = null;
      }
      vault = null;

      VaultConfig vaultConfig =
          config
              .options
              .apply(new VaultConfig())
              .environmentLoader(config.environmentLoader)
              .build();
      String token = config.tokenSupplier.getToken(config.environmentLoader, vaultConfig);
      Vault vault = new Vault(vaultConfig.token(token));
      checkVault(vault);
      LookupResponse lookupSelf = vault.auth().lookupSelf();
      LOGGER.info("Initialized new Vault");
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("More Vault details: {}", bodyAsString(lookupSelf.getRestResponse()));
      }
      if (lookupSelf.isRenewable()) {
        long ttl = lookupSelf.getTTL();
        long delay = TimeUnit.SECONDS.toMillis(suggestedRefreshInterval(ttl));
        timer = new Timer("VaultScheduler", true);
        timer.schedule(new RenewTokenTask(), delay);
        LOGGER.info("Renew token timer was set to {}s, (TTL = {}s)", delay, ttl);
      } else {
        LOGGER.warn("Vault token is not renewable");
      }
      this.vault = vault;

    } catch (VaultException e) {
      LOGGER.error("Could not initialize and validate the vault", e);
      throw new ConfigSourceNotAvailableException(e);
    }
    return vault;
  }

  private void renewToken() {
    Vault vault = this.vault;
    if (vault == null) {
      return;
    }
    try {
      AuthResponse response = vault.auth().renewSelf();
      long ttl = response.getAuthLeaseDuration();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Token was successfully renewed (new TTL = {} seconds), response: {}",
            ttl,
            bodyAsString(response.getRestResponse()));
      }
      if (ttl > 1) {
        long delay = TimeUnit.SECONDS.toMillis(suggestedRefreshInterval(ttl));
        timer.schedule(new RenewTokenTask(), delay);
      } else {
        LOGGER.warn("Token TTL ({}) is not enough for scheduling", ttl);
        vault = recreateVault(vault);
      }
    } catch (VaultException e) {
      LOGGER.error("Could not refresh the Vault token", e);
      // try recreate Vault according to https://www.vaultproject.io/api/overview#http-status-codes
      if (e.getHttpStatusCode() == 403) {
        //noinspection UnusedAssignment
        vault = recreateVault(vault);
      }
    }
  }

  /**
   * We should refresh tokens from Vault before they expire, so we add a MIN_REFRESH_MARGIN margin.
   * If the token is valid for less than MIN_REFRESH_MARGIN * 2, we use duration / 2 instead.
   */
  private long suggestedRefreshInterval(long duration) {
    return duration < MIN_REFRESH_MARGIN * 2 ? duration / 2 : duration - MIN_REFRESH_MARGIN;
  }

  private String bodyAsString(RestResponse response) {
    return new String(response.getBody(), StandardCharsets.UTF_8);
  }

  /**
   * Checks vault is active. See
   * https://www.vaultproject.io/api/system/health.html#read-health-information.
   *
   * @param vault vault
   */
  private void checkVault(Vault vault) throws VaultException {
    RestResponse restResponse = vault.debug().health().getRestResponse();
    if (restResponse.getStatus() == 200) {
      return;
    }
    String message = new String(restResponse.getBody(), StandardCharsets.UTF_8);
    throw new VaultException(message, restResponse.getStatus());
  }

  /**
   * Checks rest response. See https://www.vaultproject.io/api/overview#http-status-codes.
   *
   * @param restResponse rest response
   */
  private void checkResponse(RestResponse restResponse) throws VaultException {
    if (restResponse == null) {
      return;
    }
    int status = restResponse.getStatus();
    switch (status) {
      case 200:
      case 204:
        return;
      case 404:
        LOGGER.warn(
            "Invalid path (doesn't exist or have no permissions to view). {}",
            new String(restResponse.getBody(), StandardCharsets.UTF_8));
        return;
      default:
        String body = new String(restResponse.getBody(), StandardCharsets.UTF_8);
        LOGGER.warn("Vault responded with code: {}, message: {}", status, body);
        throw new VaultException(body, status);
    }
  }

  @FunctionalInterface
  public interface VaultCall<T extends VaultResponse> {
    T apply(Vault vault) throws VaultException;
  }

  private class RenewTokenTask extends TimerTask {

    @Override
    public void run() {
      renewToken();
    }
  }

  public static class Builder {
    public static final EnvironmentLoader ENVIRONMENT_LOADER = new EnvironmentLoader();
    public static final VaultTokenSupplier TOKEN_SUPPLIER = new EnvironmentVaultTokenSupplier();

    private Function<VaultConfig, VaultConfig> options = Function.identity();
    private VaultTokenSupplier tokenSupplier = TOKEN_SUPPLIER;
    private EnvironmentLoader environmentLoader = ENVIRONMENT_LOADER;

    /**
     * This builder method is used internally for test purposes. please use it only for tests
     *
     * @param environmentLoader an {@link EnvironmentLoader}
     */
    Builder(EnvironmentLoader environmentLoader) {
      if (environmentLoader != null) {
        this.environmentLoader = environmentLoader;
      }
    }

    public Builder options(UnaryOperator<VaultConfig> config) {
      this.options = this.options.andThen(config);
      return this;
    }

    public Builder tokenSupplier(VaultTokenSupplier supplier) {
      this.tokenSupplier = supplier;
      return this;
    }

    /**
     * Builds vault invoker.
     *
     * @return instance of {@link VaultInvoker}
     */
    public VaultInvoker build() {
      Builder builder = new Builder(environmentLoader);
      builder.options = options;
      builder.tokenSupplier = tokenSupplier;
      return new VaultInvoker(builder);
    }
  }
}
