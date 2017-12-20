package io.scalecube.config.zookeeper;

import io.scalecube.config.utils.ThrowableUtil;
import org.apache.curator.RetryPolicy;
import org.apache.curator.connection.ConnectionHandlingPolicy;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.framework.AuthInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.api.CompressionProvider;
import org.apache.curator.framework.schema.SchemaSet;
import org.apache.curator.framework.state.ConnectionStateErrorPolicy;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.utils.ZookeeperFactory;

import java.util.List;
import java.util.concurrent.ThreadFactory;

public class ZookeeperConfigConnector {

  private CuratorFramework client;

  private ZookeeperConfigConnector(Builder builder) {
    try {
      this.client = builder.factoryBuilder.build();
      this.client.start();
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }

  public static Builder forUri(String connectString) {
    return new Builder().connectString(connectString)
        .canBeReadOnly(true)
        .retryPolicy(new RetryOneTime(1000));
  }

  public CuratorFramework getClient() {
    return client;
  }

  public static class Builder {

    private CuratorFrameworkFactory.Builder factoryBuilder = CuratorFrameworkFactory.builder();

    public Builder authorization(String scheme, byte[] auth) {
      factoryBuilder.authorization(scheme, auth);
      return this;
    }

    public Builder authorization(List<AuthInfo> authInfos) {
      factoryBuilder.authorization(authInfos);
      return this;
    }

    public Builder connectString(String connectString) {
      factoryBuilder.connectString(connectString);
      return this;
    }

    public Builder ensembleProvider(EnsembleProvider ensembleProvider) {
      factoryBuilder.ensembleProvider(ensembleProvider);
      return this;
    }

    public Builder defaultData(byte[] defaultData) {
      factoryBuilder.defaultData(defaultData);
      return this;
    }

    public Builder namespace(String namespace) {
      factoryBuilder.namespace(namespace);
      return this;
    }

    public Builder sessionTimeoutMs(int sessionTimeoutMs) {
      factoryBuilder.sessionTimeoutMs(sessionTimeoutMs);
      return this;
    }

    public Builder connectionTimeoutMs(int connectionTimeoutMs) {
      factoryBuilder.connectionTimeoutMs(connectionTimeoutMs);
      return this;
    }

    public Builder maxCloseWaitMs(int maxCloseWaitMs) {
      factoryBuilder.maxCloseWaitMs(maxCloseWaitMs);
      return this;
    }

    public Builder retryPolicy(RetryPolicy retryPolicy) {
      factoryBuilder.retryPolicy(retryPolicy);
      return this;
    }

    public Builder threadFactory(ThreadFactory threadFactory) {
      factoryBuilder.threadFactory(threadFactory);
      return this;
    }

    public Builder compressionProvider(CompressionProvider compressionProvider) {
      factoryBuilder.compressionProvider(compressionProvider);
      return this;
    }

    public Builder zookeeperFactory(ZookeeperFactory zookeeperFactory) {
      factoryBuilder.zookeeperFactory(zookeeperFactory);
      return this;
    }

    public Builder aclProvider(ACLProvider aclProvider) {
      factoryBuilder.aclProvider(aclProvider);
      return this;
    }

    public Builder canBeReadOnly(boolean canBeReadOnly) {
      factoryBuilder.canBeReadOnly(canBeReadOnly);
      return this;
    }

    public Builder dontUseContainerParents() {
      factoryBuilder.dontUseContainerParents();
      return this;
    }

    public Builder connectionStateErrorPolicy(ConnectionStateErrorPolicy connectionStateErrorPolicy) {
      factoryBuilder.connectionStateErrorPolicy(connectionStateErrorPolicy);
      return this;
    }

    public Builder zk34CompatibilityMode(boolean mode) {
      factoryBuilder.zk34CompatibilityMode(mode);
      return this;
    }

    public Builder connectionHandlingPolicy(ConnectionHandlingPolicy connectionHandlingPolicy) {
      factoryBuilder.connectionHandlingPolicy(connectionHandlingPolicy);
      return this;
    }

    public Builder schemaSet(SchemaSet schemaSet) {
      factoryBuilder.schemaSet(schemaSet);
      return this;
    }

    public ZookeeperConfigConnector build() {
      return new ZookeeperConfigConnector(this);
    }
  }
}
