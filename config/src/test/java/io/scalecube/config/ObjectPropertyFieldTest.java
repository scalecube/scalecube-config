package io.scalecube.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ObjectPropertyFieldTest {

  private static final String propName = "dummy";

  @Test
  void testPrimitiveObjectPropertyField() throws Exception {
    PrimitiveClass instance = new PrimitiveClass();

    Class<PrimitiveClass> clazz = PrimitiveClass.class;
    ObjectPropertyField field_iii = new ObjectPropertyField(clazz.getDeclaredField("iii"), propName);
    ObjectPropertyField field_ddd = new ObjectPropertyField(clazz.getDeclaredField("ddd"), propName);
    ObjectPropertyField field_bbb = new ObjectPropertyField(clazz.getDeclaredField("bbb"), propName);
    ObjectPropertyField field_lll = new ObjectPropertyField(clazz.getDeclaredField("lll"), propName);

    field_iii.applyValueParser(instance, "1");
    field_ddd.applyValueParser(instance, "1E+7");
    field_bbb.applyValueParser(instance, "false");
    field_lll.applyValueParser(instance, "1");

    assertEquals(1, instance.iii);
    assertEquals(1e7, instance.ddd);
    assertFalse(instance.bbb);
    assertEquals(1, instance.lll);
  }

  @Test
  void testNonPrimitiveObjectPropertyField() throws Exception {
    NonPrimitiveClass instance = new NonPrimitiveClass();

    Class<NonPrimitiveClass> clazz = NonPrimitiveClass.class;
    ObjectPropertyField field_string = new ObjectPropertyField(clazz.getDeclaredField("str"), propName);
    ObjectPropertyField field_duration = new ObjectPropertyField(clazz.getDeclaredField("duration"), propName);

    field_string.applyValueParser(instance, "just str");
    field_duration.applyValueParser(instance, "100ms");

    assertEquals("just str", instance.str);
    assertEquals(Duration.ofMillis(100), instance.duration);
  }

  @Test
  void testListObjectPropertyField() throws Exception {
    TypedListConfigClass instance = new TypedListConfigClass();

    Class<TypedListConfigClass> clazz = TypedListConfigClass.class;
    ObjectPropertyField field_integerList = new ObjectPropertyField(clazz.getDeclaredField("integerList"), propName);

    field_integerList.applyValueParser(instance, "1,2,3");

    assertEquals(Stream.of(1, 2, 3).collect(Collectors.toList()), instance.integerList);
  }

  @Test
  void testMultimapObjectPropertyField() throws Exception {
    Map<String, List<Integer>> expectedMultimap = ImmutableMap.<String, List<Integer>>builder()
        .put("key1", ImmutableList.of(1))
        .put("key2", ImmutableList.of(2, 3, 4))
        .put("key3", ImmutableList.of(5))
        .build();
    TypedMultimapConfigClass instance = new TypedMultimapConfigClass();

    Field target = instance.getClass().getDeclaredField("integerMultimap");
    ObjectPropertyField fieldIntegerMultimap = new ObjectPropertyField(target, propName);

    fieldIntegerMultimap.applyValueParser(instance, "key1=1,key2=2,3,4,key3=5");

    assertEquals(expectedMultimap, instance.integerMultimap);
  }

  @Test
  void testUntypedListNotSupported() {
    UntypedListConfigClass instance = new UntypedListConfigClass();

    Class<UntypedListConfigClass> clazz = UntypedListConfigClass.class;

    assertThrows(IllegalArgumentException.class,
        () -> {
          ObjectPropertyField field_list = new ObjectPropertyField(clazz.getDeclaredField("list"), propName);
          field_list.applyValueParser(instance, "1,2,3");
        },
        "ObjectPropertyField: unsupported type on field");
  }

  @Test
  void testStaticOrFinalFieldsInConfigClassNotSupported() {
    Class<ConfigClassWithStaticOrFinalField> clazz = ConfigClassWithStaticOrFinalField.class;

    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      new ObjectPropertyField(clazz.getDeclaredField("defaultInstance"), propName);
      new ObjectPropertyField(clazz.getDeclaredField("finalInt"), propName);
    });

    assertTrue(
        exception.getMessage().startsWith("ObjectPropertyField: 'static' or 'final' declaration is not supported"));

  }

  private static class PrimitiveClass {
    private int iii = 0;
    private double ddd = 0;
    private boolean bbb = true;
    private long lll = 0;
  }

  private static class NonPrimitiveClass {
    private String str = "";
    private Duration duration = Duration.ofMillis(0);
  }

  private static class TypedListConfigClass {
    private List<Integer> integerList = new ArrayList<>();
  }

  private static class TypedMultimapConfigClass {
    private Map<String, List<Integer>> integerMultimap = new HashMap<>();
  }

  private static class UntypedListConfigClass {
    private List list = new ArrayList<>();
  }

  static class ConfigClassWithStaticOrFinalField {
    static final ConfigClassWithStaticOrFinalField defaultInstance = new ConfigClassWithStaticOrFinalField();

    private final int finalInt = 1;
  }
}
