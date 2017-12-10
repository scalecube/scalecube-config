package io.scalecube.config.keyvalue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import io.scalecube.config.ConfigProperty;
import io.scalecube.config.ConfigSourceNotAvailableException;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.time.Duration;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class KeyValueConfigSourceTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private KeyValueConfigRepository repository;

  private KeyValueConfigSource configSource;
  private String collectionName;
  private String g1;
  private String g2;

  @Before
  public void setup() {
    collectionName = "config";
    g1 = "group1";
    g2 = "group2";
    configSource = KeyValueConfigSource.withRepository(repository, collectionName)
        .repositoryTimeout(Duration.ofMillis(300))
        .groups(g1, g2)
        .build();
  }

  @Test
  public void testKeyValueLoadConfig() throws Exception {
    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity1 = new KeyValueConfigEntity("p1", "v1", n1);
    KeyValueConfigEntity entity2 = new KeyValueConfigEntity("p2", "v2", n2);
    KeyValueConfigEntity entity3 = new KeyValueConfigEntity("p1", "v1", root);
    KeyValueConfigEntity entity4 = new KeyValueConfigEntity("p2", "v2", root);
    KeyValueConfigEntity entity5 = new KeyValueConfigEntity("p42", "v42", root);

    when(repository.findAll(n1)).thenReturn(ImmutableList.of(entity1));
    when(repository.findAll(n2)).thenReturn(ImmutableList.of(entity2));
    when(repository.findAll(root)).thenReturn(ImmutableList.of(entity3, entity4, entity5));
    Map<String, ConfigProperty> config = configSource.loadConfig();

    assertEquals(3, config.size());
    assertEquals("v1", config.get("p1").valueAsString().get());
    assertEquals("v2", config.get("p2").valueAsString().get());
    assertEquals("v42", config.get("p42").valueAsString().get());
  }

  @Test
  public void testKeyValueLoadConfigFindAllThrowsException() throws Exception {
    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity1 = new KeyValueConfigEntity("p1", "v1", n1);
    KeyValueConfigEntity entity2 = new KeyValueConfigEntity("p2", "v2", n2);

    when(repository.findAll(n1)).thenReturn(ImmutableList.of(entity1));
    when(repository.findAll(n2)).thenReturn(ImmutableList.of(entity2));
    when(repository.findAll(root)).thenThrow(new RuntimeException("some exception"));
    Map<String, ConfigProperty> config = configSource.loadConfig();

    assertEquals(2, config.size());
    assertEquals("v1", config.get("p1").valueAsString().get());
    assertEquals("v2", config.get("p2").valueAsString().get());
  }

  @Test
  public void testKeyValueLoadConfigFindAllGettingLong() throws Exception {
    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity1 = new KeyValueConfigEntity("p1", "v1", n1);
    KeyValueConfigEntity entity2 = new KeyValueConfigEntity("p2", "v2", n2);

    when(repository.findAll(n1)).thenAnswer((Answer<ImmutableList<KeyValueConfigEntity>>) invocation -> {
      Thread.sleep(100);
      return ImmutableList.of(entity1);
    });

    when(repository.findAll(n2)).thenAnswer((Answer<ImmutableList<KeyValueConfigEntity>>) invocation -> {
      Thread.sleep(100);
      return ImmutableList.of(entity2);
    });

    when(repository.findAll(root)).thenThrow(new RuntimeException("some exception"));
    Map<String, ConfigProperty> config = configSource.loadConfig();

    assertEquals(2, config.size());
    assertEquals("v1", config.get("p1").valueAsString().get());
    assertEquals("v2", config.get("p2").valueAsString().get());
  }

  @Test
  public void testKeyValueLoadConfigFindAllRepositoryTimeout() throws Exception {
    thrown.expect(ConfigSourceNotAvailableException.class);

    KeyValueConfigName n1 = new KeyValueConfigName(g1, collectionName);
    KeyValueConfigName n2 = new KeyValueConfigName(g2, collectionName);
    KeyValueConfigName root = new KeyValueConfigName(null, collectionName);
    KeyValueConfigEntity entity = new KeyValueConfigEntity("p42", "v42", root);

    when(repository.findAll(n1)).thenAnswer((Answer<ImmutableList<KeyValueConfigEntity>>) invocation -> {
      Thread.sleep(Long.MAX_VALUE);
      throw new RuntimeException("never return");
    });

    when(repository.findAll(n2)).thenAnswer((Answer<ImmutableList<KeyValueConfigEntity>>) invocation -> {
      Thread.sleep(Long.MAX_VALUE);
      throw new RuntimeException("never return");
    });

    when(repository.findAll(root)).thenReturn(ImmutableList.of(entity));

    configSource.loadConfig();
  }
}
