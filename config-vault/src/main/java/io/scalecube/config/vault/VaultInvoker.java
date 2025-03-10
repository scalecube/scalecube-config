package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LookupResponse;
import com.bettercloud.vault.response.VaultResponse;
import com.bettercloud.vault.rest.RestResponse;
import io.scalecube.config.utils.ThrowableUtil;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultInvoker {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultInvoker.class);

  public static final int STATUS_CODE_FORBIDDEN = 403;
  public static final int STATUS_CODE_NOT_FOUND = 404;
  public static final int STATUS_CODE_HEALTH_OK = 200;
  public static final int STATUS_CODE_RESPONSE_OK = 200;
  public static final int STATUS_CODE_RESPONSE_NO_DATA = 204;

  private static final long MIN_REFRESH_MARGIN = TimeUnit.MINUTES.toSeconds(10);

  private final Builder builder;

  private Vault vault;
  private Timer timer;

  private VaultInvoker(Builder builder) {
    this.builder = builder;
  }

  public static Builder builder() {
    return new Builder();
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
      // try recreate Vault according to https://www.vaultproject.io/api/overview#http-status-codes
      if (e.getHttpStatusCode() == STATUS_CODE_FORBIDDEN) {
        LOGGER.warn(
            "Authentication failed (error message: {}), now trying to recreate vault",
            e.getMessage());
        vault = recreateVault(vault);
        return call.apply(vault);
      }
      throw e;
    }
  }

  private synchronized Vault recreateVault(Vault prev) throws VaultException {
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
          builder
              .options
              .apply(new VaultConfig().environmentLoader(new EnvironmentLoader()))
              .build();
      String token = builder.tokenSupplier.getToken(vaultConfig);
      Vault vault = new Vault(vaultConfig.token(token));
      checkVault(vault);
      LookupResponse lookupSelf = vault.auth().lookupSelf();
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
      throw e;
    }
    return vault;
  }

  private void renewToken() throws VaultException {
    Vault vault = this.vault;
    if (vault == null) {
      return;
    }
    try {
      AuthResponse response = vault.auth().renewSelf();
      long ttl = response.getAuthLeaseDuration();
      LOGGER.debug("Token was successfully renewed (new TTL = {}s)", ttl);
      if (response.isAuthRenewable()) {
        if (ttl > 1) {
          long delay = TimeUnit.SECONDS.toMillis(suggestedRefreshInterval(ttl));
          timer.schedule(new RenewTokenTask(), delay);
        } else {
          LOGGER.warn("Token TTL ({}s) is not enough for scheduling", ttl);
          vault = recreateVault(vault);
        }
      } else {
        LOGGER.warn("Vault token is not renewable now");
      }
    } catch (VaultException e) {
      // try recreate Vault according to https://www.vaultproject.io/api/overview#http-status-codes
      if (e.getHttpStatusCode() == STATUS_CODE_FORBIDDEN) {
        LOGGER.warn("Could not renew the Vault token", e);
        //noinspection UnusedAssignment
        vault = recreateVault(vault);
      }
    }
  }

  /**
   * Checks vault is active. See
   * https://www.vaultproject.io/api/system/health.html#read-health-information.
   *
   * @param vault vault
   */
  private void checkVault(Vault vault) throws VaultException {
    RestResponse restResponse = vault.debug().health(true, null, null, null).getRestResponse();
    if (restResponse.getStatus() == STATUS_CODE_HEALTH_OK) {
      return;
    }
    throw new VaultException(bodyAsString(restResponse), restResponse.getStatus());
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
      case STATUS_CODE_RESPONSE_OK:
      case STATUS_CODE_RESPONSE_NO_DATA:
        return;
      default:
        LOGGER.warn("Vault responded with code: {}", status);
        throw new VaultException(bodyAsString(restResponse), status);
    }
  }

  /**
   * We should refresh tokens from Vault before they expire, so we add a MIN_REFRESH_MARGIN margin.
   * If the token is valid for less than MIN_REFRESH_MARGIN * 2, we use duration / 2 instead.
   */
  private static long suggestedRefreshInterval(long duration) {
    return duration < MIN_REFRESH_MARGIN * 2 ? duration / 2 : duration - MIN_REFRESH_MARGIN;
  }

  private static String bodyAsString(RestResponse response) {
    return new String(response.getBody(), StandardCharsets.UTF_8);
  }

  @FunctionalInterface
  public interface VaultCall<T extends VaultResponse> {

    T apply(Vault vault) throws VaultException;
  }

  private class RenewTokenTask extends TimerTask {

    @Override
    public void run() {
      try {
        renewToken();
      } catch (Exception e) {
        throw ThrowableUtil.propagate(e);
      }
    }
  }

  public static class Builder {

    private static final int OPEN_TIMEOUT_SEC =
        Optional.ofNullable(System.getenv("VAULT_OPEN_TIMEOUT")).map(Integer::parseInt).orElse(10);

    private static final int READ_TIMEOUT_SEC =
        Optional.ofNullable(System.getenv("VAULT_READ_TIMEOUT")).map(Integer::parseInt).orElse(10);

    private Function<VaultConfig, VaultConfig> options =
        config -> config.openTimeout(OPEN_TIMEOUT_SEC).readTimeout(READ_TIMEOUT_SEC);

    private VaultTokenSupplier tokenSupplier = new EnvironmentVaultTokenSupplier();

    private Builder() {}

    /**
     * Setter for {@link VaultConfig} operator.
     *
     * @param operator operator for {@link VaultConfig}
     * @return this
     */
    public Builder options(UnaryOperator<VaultConfig> operator) {
      options = options.andThen(operator);
      return this;
    }

    /**
     * Setter for {@link VaultTokenSupplier}.
     *
     * @param supplier vault token supplier
     * @return this
     */
    public Builder tokenSupplier(VaultTokenSupplier supplier) {
      tokenSupplier = supplier;
      return this;
    }

    /**
     * Builds vault invoker.
     *
     * @return instance of {@link VaultInvoker}
     */
    public VaultInvoker build() {
      Builder builder = new Builder();
      builder.options = options;
      builder.tokenSupplier = tokenSupplier;
      return new VaultInvoker(builder);
    }
  }
}
