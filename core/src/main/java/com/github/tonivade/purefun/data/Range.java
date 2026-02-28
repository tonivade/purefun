/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.check;
import static com.github.tonivade.purefun.core.Precondition.greaterThanOrEquals;
import static com.github.tonivade.purefun.type.Validation.mapN;
import static com.github.tonivade.purefun.type.Validation.requireGreaterThanOrEqual;
import static com.github.tonivade.purefun.type.Validation.requireLowerThan;
import static com.github.tonivade.purefun.type.Validation.requireLowerThanOrEqual;
import static com.github.tonivade.purefun.type.Validation.requireNonEquals;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;

public record Range(int begin, int end, int increment) implements Iterable<Integer> {

  public Range(int begin, int end) {
    this(begin, end, 1);
  }

  public Range {
    check(() -> increment != 0);
    if (increment > 0) {
      check(greaterThanOrEquals(end, begin));
    } else {
      check(greaterThanOrEquals(begin, end));
    }
  }

  public Range reverse() {
    return new Range(end, begin, -increment);
  }

  public boolean contains(int value) {
    if (increment < 0) {
      return mapN(
          requireGreaterThanOrEqual(value, end),
          requireLowerThan(value, begin), Tuple::of).isValid();
    }
    return mapN(
        requireGreaterThanOrEqual(value, begin),
        requireLowerThan(value, end), Tuple::of).isValid();
  }

  public int size() {
    return Math.abs(end - begin);
  }

  public ImmutableArray<Integer> collect() {
    return map(identity());
  }

  public <T> ImmutableArray<T> map(Function1<? super Integer, ? extends T> map) {
    return ImmutableArray.from(intStream().boxed()::iterator).map(map);
  }

  public IntStream intStream() {
    if (increment < 0) {
      return IntStream.range(end, begin).map(i -> begin - i + end - 1);
    }
    return IntStream.range(begin, end);
  }

  public Stream<Integer> stream() {
    return intStream().boxed();
  }

  @Override
  public Iterator<Integer> iterator() {
    return intStream().iterator();
  }

  @Override
  public String toString() {
    return String.format("Range(%d..%d, increment=%d)", begin, end, increment);
  }

  public static Range of(int begin, int end) {
    return mapN(
        requireLowerThanOrEqual(begin, end),
        requireGreaterThanOrEqual(end, begin), Range::new).getOrElseThrow();
  }

  public static Range of(int begin, int end, int increment) {
    return mapN(
        requireLowerThanOrEqual(begin, end),
        requireGreaterThanOrEqual(end, begin),
        requireNonEquals(increment, 0), Range::new).getOrElseThrow();
  }
}
