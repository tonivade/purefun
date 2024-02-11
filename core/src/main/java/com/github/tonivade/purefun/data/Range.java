/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Tuple;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.check;
import static com.github.tonivade.purefun.core.Precondition.greaterThanOrEquals;
import static com.github.tonivade.purefun.type.Validation.mapN;
import static com.github.tonivade.purefun.type.Validation.requireGreaterThanOrEqual;
import static com.github.tonivade.purefun.type.Validation.requireLowerThan;
import static com.github.tonivade.purefun.type.Validation.requireLowerThanOrEqual;

public record Range(int begin, int end) implements Iterable<Integer> {

  public Range {
    check(greaterThanOrEquals(end, begin));
  }

  public boolean contains(int value) {
    return mapN(
        requireGreaterThanOrEqual(value, begin),
        requireLowerThan(value, end), Tuple::of).isValid();
  }

  public int size() {
    return end - begin;
  }

  public Sequence<Integer> collect() {
    return map(identity());
  }

  public <T> Sequence<T> map(Function1<? super Integer, ? extends T> map) {
    return ImmutableArray.from(intStream().boxed()).map(map);
  }

  public IntStream intStream() {
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
    return String.format("Range(%d..%d)", begin, end);
  }

  public static Range of(int begin, int end) {
    return mapN(
        requireLowerThanOrEqual(begin, end),
        requireGreaterThanOrEqual(end, begin), Range::new).getOrElseThrow();
  }
}
