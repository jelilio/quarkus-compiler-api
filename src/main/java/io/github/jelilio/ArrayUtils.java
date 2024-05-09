package io.github.jelilio;

import java.util.stream.Stream;

public final class ArrayUtils {
  public static String[] merge(String[] ...arrays) {
    return Stream.of(arrays)
        .flatMap(Stream::of)        // or, use `Arrays::stream`
        .toArray(String[]::new);
  }
}
