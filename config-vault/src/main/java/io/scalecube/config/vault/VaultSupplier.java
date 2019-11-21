package io.scalecube.config.vault;

import com.bettercloud.vault.EnvironmentLoader;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LookupResponse;
import io.scalecube.config.utils.ThrowableUtil;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultSupplier implements Supplier<Vault> {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultSupplier.class);

  private static final long MIN_REFRESH_MARGIN = TimeUnit.MINUTES.toSeconds(10);
  private static final Timer TIMER = new Timer("VaultScheduler", true);

  private final Function<VaultConfig, VaultConfig> config;
  private final VaultTokenSupplier tokenSupplier;
  private final EnvironmentLoader environmentLoader;
  private final TimerTask refreshTokenTask;

  private Vault vault;

  public VaultSupplier(
      Function<VaultConfig, VaultConfig> config, VaultTokenSupplier tokenSupplier) {
    this(config, tokenSupplier, null);
  }

  VaultSupplier(
      Function<VaultConfig, VaultConfig> config,
      VaultTokenSupplier tokenSupplier,
      EnvironmentLoader environmentLoader) {
    this.config = config;
    this.tokenSupplier = tokenSupplier;
    this.environmentLoader =
        environmentLoader != null ? environmentLoader : new EnvironmentLoader();
    this.refreshTokenTask =
        new TimerTask() {
          @Override
          public void run() {
            refreshToken();
          }
        };
    initVault();
  }

  @Override
  public Vault get() {
    return vault;
  }

  private void refreshToken() {
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Refreshing Vault token (old TTL = {} seconds)", vault.auth().lookupSelf().getTTL());
      }
      AuthResponse response = vault.auth().renewSelf();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Refreshed Vault token (new TTL = {} seconds)", vault.auth().lookupSelf().getTTL());
      }
      TIMER.schedule(refreshTokenTask, suggestedRefreshInterval(response.getAuthLeaseDuration()));
    } catch (VaultException e) {
      LOGGER.error("Could not refresh the Vault token", e);
      initVault();
    }
  }

  private void initVault() {
    try {
      VaultConfig vaultConfig =
          config.apply(new VaultConfig()).environmentLoader(environmentLoader).build();
      String token = tokenSupplier.getToken(environmentLoader, vaultConfig);
      vault = new Vault(vaultConfig.token(token));
    } catch (VaultException e) {
      LOGGER.error("Could not initialize the vault", e);
      throw ThrowableUtil.propagate(e);
    }

    try {
      LookupResponse lookupSelf = vault.auth().lookupSelf();
      if (lookupSelf.isRenewable()) {
        final long ttl = lookupSelf.getTTL();
        LOGGER.info("Starting a refresh timer on the vault token (TTL = {} seconds", ttl);
        TIMER.schedule(refreshTokenTask, suggestedRefreshInterval(ttl));
      } else {
        LOGGER.warn("Vault token is not renewable");
      }
    } catch (VaultException e) {
      if (e.getHttpStatusCode() == 403) {
        LOGGER.error("The application's vault token seems to be invalid", e);
      } else {
        LOGGER.error("Could not validate the application's vault token", e);
      }
      throw ThrowableUtil.propagate(e);
    }
  }

  /**
   * We should refresh tokens from Vault before they expire, so we add a MIN_REFRESH_MARGIN margin.
   * If the token is valid for less than MIN_REFRESH_MARGIN * 2, we use duration / 2 instead.
   */
  private long suggestedRefreshInterval(long duration) {
    if (duration < MIN_REFRESH_MARGIN * 2) {
      return duration / 2;
    } else {
      return duration - MIN_REFRESH_MARGIN;
    }
  }
}
