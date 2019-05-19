package io.scalecube.config.keyvalue;

import java.util.List;

/** Generic key-value config data access interface. */
public interface KeyValueConfigRepository {

  /**
   * Retrieves all key-value pairs under given config name.
   *
   * @param configName a config name.
   * @return list of key-value entries.
   * @throws Exception in case of any issue happened when accessing config data source.
   */
  List<KeyValueConfigEntity> findAll(KeyValueConfigName configName) throws Exception;
}
