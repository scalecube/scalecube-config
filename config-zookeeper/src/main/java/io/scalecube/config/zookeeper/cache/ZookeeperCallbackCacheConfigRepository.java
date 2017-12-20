package io.scalecube.config.zookeeper.cache;

import io.scalecube.config.keyvalue.KeyValueConfigEntity;
import io.scalecube.config.keyvalue.KeyValueConfigName;
import io.scalecube.config.keyvalue.KeyValueConfigRepository;
import io.scalecube.config.utils.ThrowableUtil;
import io.scalecube.config.zookeeper.ZookeeperConfigConnector;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.shaded.com.google.common.base.Preconditions;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ZookeeperCallbackCacheConfigRepository implements KeyValueConfigRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperCallbackCacheConfigRepository.class);

  private final CuratorFramework client;

  private Map<KeyValueConfigName, ConfigRepository> repositories = new ConcurrentHashMap<>();

  public ZookeeperCallbackCacheConfigRepository(@Nonnull ZookeeperConfigConnector connector) {
    this.client = Objects.requireNonNull(connector).getClient();
  }

  @Override
  public List<KeyValueConfigEntity> findAll(@Nonnull KeyValueConfigName configName) {
    return new ArrayList<>(repositories.computeIfAbsent(configName, ConfigRepository::new).props.values());
  }

  private final class ConfigRepository {
    private final KeyValueConfigName configName;
    private final String rootPath;
    private final TreeNode root;
    private final Map<String, KeyValueConfigEntity> props;

    private ConfigRepository(KeyValueConfigName configName) {
      this.configName = configName;
      this.rootPath = resolvePath(configName);
      this.root = new TreeNode(rootPath, null);
      this.props = new ConcurrentHashMap<>(1);
      this.root.warmup();
    }

    private String resolvePath(KeyValueConfigName configName) {
      return configName.getGroupName().map(group -> "/" + group + "/").orElse("/") + configName.getCollectionName();
    }

    private String propertyName(String fullPath) {
      return fullPath.substring(rootPath.length() + "/".length()).replace("/", ".");
    }

    private final class TreeNode implements Watcher, BackgroundCallback {
      private final String path;
      private final TreeNode parent;
      private final Map<String, TreeNode> children = new ConcurrentHashMap<>();

      private TreeNode(String path, TreeNode parent) {
        this.path = path;
        this.parent = parent;
      }

      @Override
      public void process(WatchedEvent event) {
        try {
          switch (event.getState()) {
            case SyncConnected:
              switch (event.getType()) {
                case NodeCreated:
                  Preconditions.checkState(isRoot(), "unexpected NodeCreated on non-root node");
                  wasCreated();
                  break;
                case NodeChildrenChanged:
                  refreshChildren();
                  break;
                case NodeDataChanged:
                  refreshData();
                  break;
                case NodeDeleted:
                  wasDeleted();
                  break;
                case None:
                  wasReconnected();
                  break;
              }
              break;
            case Disconnected:
              wasDisconnected();
              break;
          }
        } catch (Exception e) {
          LOGGER.error("Exception occurred: " + e, e);
        }
      }

      @Override
      public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
        switch (event.getType()) {
          case EXISTS:
            Preconditions.checkState(isRoot(), "unexpected EXISTS on non-root node");
            if (event.getResultCode() == KeeperException.Code.OK.intValue()) {
              wasCreated();
            }
            break;
          case CHILDREN:
            if (event.getResultCode() == KeeperException.Code.OK.intValue()) {
              event.getChildren().stream()
                  .filter(child -> !children.containsKey(child))
                  .forEach(child -> {
                    String fullPath = ZKPaths.makePath(path, child);
                    TreeNode node = newChildNode(fullPath);
                    if (children.putIfAbsent(child, node) == null) {
                      node.wasCreated();
                    }
                  });
            } else if (event.getResultCode() == KeeperException.Code.NONODE.intValue()) {
              wasDeleted();
            }
            break;
          case GET_DATA:
            if (event.getResultCode() == KeeperException.Code.OK.intValue()) {
              putData(event.getData());
            } else if (event.getResultCode() == KeeperException.Code.NONODE.intValue()) {
              wasDeleted();
            }
            break;
          default:
            LOGGER.warn("Unknown event: " + event);
        }
      }

      private boolean isRoot() {
        return parent == null;
      }

      private void warmup() {
        try {
          if (!isRoot()) {
            putData(client.getData().usingWatcher(this).forPath(path));
          }
          List<String> children = client.getChildren().usingWatcher(this).forPath(path);
          for (String child : children) {
            String fullPath = ZKPaths.makePath(path, child);
            TreeNode node = newChildNode(fullPath);
            this.children.put(child, node);
            node.warmup();
          }
        } catch (Exception e) {
          LOGGER.error("Exception occurred: " + e, e);
          throw ThrowableUtil.propagate(e);
        }
      }

      private TreeNode newChildNode(String fullPath) {
        return new TreeNode(fullPath, this);
      }

      private void refreshChildren() {
        try {
          client.getChildren().usingWatcher(this).inBackground(this).forPath(path);
        } catch (Exception e) {
          LOGGER.error("Exception occurred: " + e, e);
        }
      }

      private void refreshData() {
        try {
          client.getData().usingWatcher(this).inBackground(this).forPath(path);
        } catch (Exception e) {
          LOGGER.error("Exception occurred: " + e, e);
        }
      }

      private void wasCreated() {
        refreshChildren();
        refreshData();
      }

      private void wasDeleted() {
        try {
          if (isRoot()) {
            client.checkExists().usingWatcher(this).inBackground(this).forPath(path);
          } else {
            repositories.get(configName).props.remove(propertyName(path));
            parent.children.remove(ZKPaths.getNodeFromPath(path), this);
          }
          children.forEach((path, child) -> child.wasDeleted());
          children.clear();
        } catch (Exception e) {
          LOGGER.error("Exception occurred: " + e, e);
        }
      }

      private void wasDisconnected() {
        if (isRoot()) {
          repositories.remove(configName);
        }
      }

      private void wasReconnected() {
        if (isRoot()) {
          refreshChildren();
        }
      }

      private void putData(byte[] data) {
        if (data != null && data.length > 0) {
          KeyValueConfigEntity entity = new KeyValueConfigEntity().setConfigName(configName);
          entity.setPropName(propertyName(path));
          entity.setPropValue(new String(data));
          props.put(entity.getPropName(), entity);
        }
      }
    }
  }
}
