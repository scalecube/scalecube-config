package io.scalecube.config.keyvalue;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface KeyValueConfigRepository {

  List<KeyValueConfigEntity> findAll(@Nullable String groupName, @Nonnull String collectionName);
}
