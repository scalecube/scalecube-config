package io.scalecube.config.zookeeper;

import io.scalecube.config.utils.ThrowableUtil;
import org.apache.zookeeper.ZooKeeper;

public class ZookeeperConfigConnector {

  public static final int DEFAULT_SESSION_TIMEOUT = 5000;

  private final ZooKeeper zooKeeper;

  private ZookeeperConfigConnector(Builder builder) {
    try {
      this.zooKeeper = new ZooKeeper(builder.connectionString, builder.sessionTimeout, null, true);
    } catch (Exception e) {
      throw ThrowableUtil.propagate(e);
    }
  }

  public ZooKeeper getClient() {
    return zooKeeper;
  }

  public static class Builder {

    private String connectionString;
    private int sessionTimeout = DEFAULT_SESSION_TIMEOUT;

    public static Builder forUri(String uri) {
      return new Builder().connectionString(uri);
    }

    public Builder connectionString(String connectionString) {
      this.connectionString = connectionString;
      return this;
    }

    public Builder sessionTimeout(int sessionTimeout) {
      this.sessionTimeout = sessionTimeout;
      return this;
    }

    public ZookeeperConfigConnector build() {
      return new ZookeeperConfigConnector(this);
    }
  }
}
