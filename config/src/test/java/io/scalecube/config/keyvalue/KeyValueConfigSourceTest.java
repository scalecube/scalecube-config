package io.scalecube.config.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.common.collect.ImmutableList;
import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class KeyValueConfigSourceTest {

  @Mock private KeyValueConfigRepository repository;

  private KeyValueConfigSource configSource;
  private String collectionName;
  private String g1;
  private String g2;

  @BeforeEach
  void setup() {
    collectionName = "config";
    g1 = "group1";
    g2 = "group2";
    configSource =
        KeyValueConfigSource.withRepository(repository, collectionName)
            .repositoryTimeout(Duration.ofMillis(300))
            .groups(g1, g2)
            .build();
  }

  @Test
  void testKeyValueLoadConfig() throws Exception {
    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity1 = new KeyValueConfigEntity("p1", "v1", n1);
    KeyValueConfigEntity entity2 = new KeyValueConfigEntity("p2", "v2", n2);
    KeyValueConfigEntity entity3 = new KeyValueConfigEntity("p1", "v1", root);
    KeyValueConfigEntity entity4 = new KeyValueConfigEntity("p2", "v2", root);
    KeyValueConfigEntity entity5 = new KeyValueConfigEntity("p42", "v42", root);

    doReturn(ImmutableList.of(entity1)).when(repository).findAll(n1);
    doReturn(ImmutableList.of(entity2)).when(repository).findAll(n2);
    doReturn(ImmutableList.of(entity3, entity4, entity5)).when(repository).findAll(root);

    Map<String, ConfigProperty> config = configSource.loadConfig();

    assertEquals(3, config.size());
    assertEquals("v1", config.get("p1").valueAsString().get());
    assertEquals("v2", config.get("p2").valueAsString().get());
    assertEquals("v42", config.get("p42").valueAsString().get());
  }

  @Test
  void testKeyValueLoadConfigFindAllThrowsException() throws Exception {
    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity1 = new KeyValueConfigEntity("p1", "v1", n1);
    KeyValueConfigEntity entity2 = new KeyValueConfigEntity("p2", "v2", n2);

    doReturn(ImmutableList.of(entity1)).when(repository).findAll(n1);
    doReturn(ImmutableList.of(entity2)).when(repository).findAll(n2);
    doThrow(new RuntimeException("some exception")).when(repository).findAll(root);

    Map<String, ConfigProperty> config = configSource.loadConfig();

    assertEquals(2, config.size());
    assertEquals("v1", config.get("p1").valueAsString().get());
    assertEquals("v2", config.get("p2").valueAsString().get());
  }

  @Test
  void testKeyValueLoadConfigFindAllGettingLong() throws Exception {
    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity1 = new KeyValueConfigEntity("p1", "v1", n1);
    KeyValueConfigEntity entity2 = new KeyValueConfigEntity("p2", "v2", n2);

    doAnswer(
            (Answer<ImmutableList<KeyValueConfigEntity>>)
                invocation -> {
                  Thread.sleep(100);
                  return ImmutableList.of(entity1);
                })
        .when(repository)
        .findAll(n1);

    doAnswer(
            (Answer<ImmutableList<KeyValueConfigEntity>>)
                invocation -> {
                  Thread.sleep(100);
                  return ImmutableList.of(entity2);
                })
        .when(repository)
        .findAll(n2);

    doThrow(new RuntimeException("some exception")).when(repository).findAll(root);

    Map<String, ConfigProperty> config = configSource.loadConfig();

    assertEquals(2, config.size());
    assertEquals("v1", config.get("p1").valueAsString().get());
    assertEquals("v2", config.get("p2").valueAsString().get());
  }

  @Test
  void testKeyValueLoadConfigFindAllRepositoryTimeout() throws Exception {
    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity = new KeyValueConfigEntity("p42", "v42", root);

    doAnswer(
            (Answer<ImmutableList<KeyValueConfigEntity>>)
                invocation -> {
                  Thread.sleep(Long.MAX_VALUE);
                  throw new RuntimeException("never return");
                })
        .when(repository)
        .findAll(n1);

    doAnswer(
            (Answer<ImmutableList<KeyValueConfigEntity>>)
                invocation -> {
                  Thread.sleep(Long.MAX_VALUE);
                  throw new RuntimeException("never return");
                })
        .when(repository)
        .findAll(n2);

    doReturn(ImmutableList.of(entity)).when(repository).findAll(root);

    assertThrows(ConfigSourceNotAvailableException.class, configSource::loadConfig);
  }
}
