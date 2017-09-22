package io.scalecube.config;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectPropertyFieldTest {
  static final String propName = "dummy";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testPrimitiveObjectPropertyField() throws Exception {
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
    assertEquals(1e7, instance.ddd, 0);
    assertEquals(false, instance.bbb);
    assertEquals(1, instance.lll);
  }

  @Test
  public void testNonPrimitiveObjectPropertyField() throws Exception {
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
  public void testListObjectPropertyField() throws Exception {
    TypedListConfigClass instance = new TypedListConfigClass();

    Class<TypedListConfigClass> clazz = TypedListConfigClass.class;
    ObjectPropertyField field_integerList = new ObjectPropertyField(clazz.getDeclaredField("integerList"), propName);

    field_integerList.applyValueParser(instance, "1,2,3");

    assertEquals(Stream.of(1, 2, 3).collect(Collectors.toList()), instance.integerList);
  }

  @Test
  public void testUntypedListNotSupported() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("ObjectPropertyField: unsupported type on field");

    UntypedListConfigClass instance = new UntypedListConfigClass();

    Class<UntypedListConfigClass> clazz = UntypedListConfigClass.class;
    ObjectPropertyField field_list = new ObjectPropertyField(clazz.getDeclaredField("list"), propName);

    field_list.applyValueParser(instance, "1,2,3");
  }

  @Test
  public void testStaticOrFinalFieldsInConfigClassNotSupported() throws Exception {
    Class<ConfigClassWithStaticOrFinalField> clazz = ConfigClassWithStaticOrFinalField.class;
    try {
      new ObjectPropertyField(clazz.getDeclaredField("defaultInstance"), propName);
      fail("Expect fail here with IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), startsWith("ObjectPropertyField: 'static' or 'final' declaration is not supported"));
    }
    try {
      new ObjectPropertyField(clazz.getDeclaredField("finalInt"), propName);
      fail("Expect fail here with IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), startsWith("ObjectPropertyField: 'static' or 'final' declaration is not supported"));
    }
  }

  static class PrimitiveClass {
    private int iii = 0;
    private double ddd = 0;
    private boolean bbb = true;
    private long lll = 0;
  }

  static class NonPrimitiveClass {
    private String str = "";
    private Duration duration = Duration.ofMillis(0);
  }

  static class TypedListConfigClass {
    private List<Integer> integerList = new ArrayList<>();
  }

  static class UntypedListConfigClass {
    private List list = new ArrayList<>();
  }

  static class ConfigClassWithStaticOrFinalField {
    static final ConfigClassWithStaticOrFinalField defaultInstance = new ConfigClassWithStaticOrFinalField();

    private final int finalInt = 1;
  }
}
