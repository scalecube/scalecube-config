package io.scalecube.config.keyvalue;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Generic key-value data access interface.
 */
public interface KeyValueConfigRepository {

  /**
   * Retrieves all key-value objects under given group name in given collection.
   *
   * @param groupName group name; may be null.
   * @param collectionName data source collection name.
   * @return list of key-value entity objects.
   */
  List<KeyValueConfigEntity> findAll(@Nullable String groupName, @Nonnull String collectionName);
}
