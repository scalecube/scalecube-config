package io.scalecube.config.vault;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.vault.VaultContainer;

public class VaultContainerExtension
    implements AfterAllCallback, BeforeAllCallback, AfterEachCallback {

  private VaultInstance vaultInstance;
  private List<VaultInstance> vaultInstances = new ArrayList<>();

  @Override
  public void beforeAll(ExtensionContext context) {
    vaultInstance = startNewVaultInstance();
    vaultInstances.clear();
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (vaultInstance != null && vaultInstance.container().isRunning()) {
      vaultInstance.container().stop();
    }
  }

  @Override
  public void afterEach(ExtensionContext context) {
    vaultInstances.forEach(VaultInstance::close);
    vaultInstances.clear();
  }

  VaultInstance vaultInstance() {
    return vaultInstance;
  }

  VaultInstance startNewVaultInstance() {
    return startNewVaultInstance(UnaryOperator.identity());
  }

  VaultInstance startNewVaultInstance(UnaryOperator<VaultContainer> function) {
    VaultInstance vaultInstance = VaultInstance.start(function);
    vaultInstances.add(vaultInstance);
    return vaultInstance;
  }
}
