package io.scalecube.config.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.rest.RestResponse;
import io.scalecube.config.utils.ThrowableUtil;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.LogUtils;
import org.testcontainers.vault.VaultContainer;

public class VaultInstance implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(VaultInstance.class);

  private static final String VAULT_IMAGE_NAME = "vault:1.2.3";
  private static final int VAULT_PORT = 8200;
  private static final AtomicInteger PORT_COUNTER = new AtomicInteger(VAULT_PORT);
  private static final String UNSEAL_KEY_LOG = "Unseal Key: ";
  private static final String ROOT_TOKEN_LOG = "Root Token: ";

  private final VaultContainer container;
  private final String unsealKey;
  private final String rootToken;

  private VaultInstance(VaultContainer container, String unsealKey, String rootToken) {
    this.container = container;
    this.unsealKey = Objects.requireNonNull(unsealKey, "unseal key");
    this.rootToken = Objects.requireNonNull(rootToken, "root token");
  }

  static VaultInstance start(UnaryOperator<VaultContainer> function) {
    VaultInstanceWaitStrategy waitStrategy = new VaultInstanceWaitStrategy();
    VaultContainer container =
        function.apply(
            new VaultContainer<>(VAULT_IMAGE_NAME)
                .withVaultToken(UUID.randomUUID().toString())
                .withVaultPort(PORT_COUNTER.incrementAndGet())
                .waitingFor(waitStrategy));
    container.start();
    return new VaultInstance(container, waitStrategy.unsealKey, waitStrategy.rootToken);
  }

  public VaultContainer container() {
    return container;
  }

  public Vault vault() {
    return invoke(
        () -> {
          String vaultToken = container.getEnvMap().get("VAULT_TOKEN").toString();
          VaultConfig config =
              new VaultConfig()
                  .address(address())
                  .token(vaultToken)
                  .openTimeout(5)
                  .readTimeout(30)
                  .sslConfig(new SslConfig().build())
                  .build();
          return new Vault(config).withRetries(5, 1000);
        });
  }

  public void putSecrets(String path, String firstSecret, String... remainingSecrets) {
    StringBuilder command =
        new StringBuilder()
            .append("vault kv put ")
            .append(path)
            .append(" ")
            .append(firstSecret)
            .append(" ");
    for (String secret : remainingSecrets) {
      command.append(secret).append(" ");
    }
    ExecResult execResult =
        invoke(() -> container.execInContainer("/bin/sh", "-c", command.toString()));
    assertEquals(0, execResult.getExitCode(), execResult.toString());
  }

  /**
   * Creates a new token with given options. See
   * https://www.vaultproject.io/docs/commands/token/create.html.
   *
   * @param options command options,
   *     https://www.vaultproject.io/docs/commands/token/create.html#command-options
   * @return key-value result outcome
   */
  public AuthResponse createToken(String... options) {
    StringBuilder command = new StringBuilder().append("vault token create -format=json ");
    for (String secret : options) {
      command.append(secret).append(" ");
    }
    String stdout = execInContainer(command.toString()).replaceAll("\\r?\\n", "");
    return new AuthResponse(
        new RestResponse(200, "application/json", stdout.getBytes(StandardCharsets.UTF_8)), 0);
  }

  public String execInContainer(String command) {
    LOGGER.debug("execInContainer command: {}", command);
    ExecResult execResult = invoke(() -> container.execInContainer("/bin/sh", "-c", command));
    assertEquals(0, execResult.getExitCode(), execResult.toString());
    LOGGER.debug("execInContainer result: {}", execResult.getStdout());
    return execResult.getStdout();
  }

  public String address() {
    return String.format(
        "http://%s:%d", container.getContainerIpAddress(), container.getMappedPort(VAULT_PORT));
  }

  public String rootToken() {
    return rootToken;
  }

  public String unsealKey() {
    return unsealKey;
  }

  public void close() {
    container.close();
  }

  private <T> T invoke(Callable<T> action) {
    try {
      return action.call();
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }

  private static class VaultInstanceWaitStrategy extends AbstractWaitStrategy {

    private static final String VAULT_STARTED_LOG_MESSAGE =
        "==> Vault server started! Log data will stream in below:";

    private boolean isVaultStarted;
    private String unsealKey;
    private String rootToken;

    @Override
    protected void waitUntilReady() {
      WaitingConsumer waitingConsumer = new WaitingConsumer();
      LogUtils.followOutput(
          DockerClientFactory.instance().client(),
          waitStrategyTarget.getContainerId(),
          waitingConsumer);

      Predicate<OutputFrame> waitPredicate =
          outputFrame -> {
            String log = outputFrame.getUtf8String();
            if (log.contains(UNSEAL_KEY_LOG)) {
              unsealKey = log.substring(UNSEAL_KEY_LOG.length()).replaceAll("\\r?\\n", "");
            }
            if (log.contains(ROOT_TOKEN_LOG)) {
              rootToken = log.substring(ROOT_TOKEN_LOG.length()).replaceAll("\\r?\\n", "");
            }
            if (log.contains(VAULT_STARTED_LOG_MESSAGE)) {
              isVaultStarted = true;
            }
            return isVaultStarted && unsealKey != null && rootToken != null;
          };

      try {
        waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, 1);
      } catch (TimeoutException e) {
        throw new ContainerLaunchException(
            "Timed out waiting for log output matching '" + VAULT_STARTED_LOG_MESSAGE + "'");
      }
    }
  }
}
