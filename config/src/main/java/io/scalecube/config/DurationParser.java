package io.scalecube.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * {@link java.time.Duration} parser. Recognizes string format from <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence-"
 * target= "_blank">official java8 documentation</a> and more convenient string form with integer
 * value followed by time_unit string (which is one of: <b>ns</b> - nanos, <b>us</b> - micros,
 * <b>ms</b> - millis, <b>s</b> - seconds, <b>m</b> - minutes, <b>h</b> - hours, <b>d</b> - days).
 */
class DurationParser {

  private DurationParser() {
    // Do not instantiate
  }

  // adapted from
  // https://github.com/typesafehub/config/blob/v1.3.0/config/src/main/java/com/typesafe/config/impl/SimpleConfig.java#L551-L624
  static Duration parseDuration(String input) {
    if (input.startsWith("P") || input.startsWith("-P") || input.startsWith("+P")) {
      return Duration.parse(input);
    }

    String[] parts = splitNumericAndChar(input);
    String numberString = parts[0];
    String originalUnitString = parts[1];
    String unitString = originalUnitString;

    if (numberString.length() == 0) {
      throw new IllegalArgumentException(String.format("No number in duration value '%s'", input));
    }

    if (unitString.length() > 2 && !unitString.endsWith("s")) {
      unitString = unitString + "s";
    }

    ChronoUnit units;
    switch (unitString) {
      case "ns":
        units = ChronoUnit.NANOS;
        break;
      case "us":
        units = ChronoUnit.MICROS;
        break;
      case "":
      case "ms":
        units = ChronoUnit.MILLIS;
        break;
      case "s":
        units = ChronoUnit.SECONDS;
        break;
      case "m":
        units = ChronoUnit.MINUTES;
        break;
      case "h":
        units = ChronoUnit.HOURS;
        break;
      case "d":
        units = ChronoUnit.DAYS;
        break;
      default:
        throw new IllegalArgumentException(
            String.format(
                "Could not parse time unit '%s' (try ns, us, ms, s, m, h, d)", originalUnitString));
    }

    return Duration.of(Long.parseLong(numberString), units);
  }

  // adapted from
  // https://github.com/typesafehub/config/blob/v1.3.0/config/src/main/java/com/typesafe/config/impl/ConfigImplUtil.java#L118-L164
  private static String[] splitNumericAndChar(String input) {
    int i = input.length() - 1;
    while (i >= 0) {
      char c = input.charAt(i);
      if (!Character.isLetter(c)) {
        break;
      }
      i -= 1;
    }
    return new String[] {input.substring(0, i + 1).trim(), input.substring(i + 1).trim()};
  }
}
