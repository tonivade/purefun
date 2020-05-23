/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Tuple;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.type.Validation.map2;
import static com.github.tonivade.purefun.type.Validation.requireGreaterThanOrEqual;
import static com.github.tonivade.purefun.type.Validation.requireLowerThan;
import static com.github.tonivade.purefun.type.Validation.requireLowerThanOrEqual;

public final class Range implements Iterable<Integer>, Serializable {

  private static final long serialVersionUID = 7923835507243835436L;

  private static final Equal<Range> EQUAL = Equal.<Range>of().comparing(x -> x.begin).comparing(x -> x.end);

  private final int begin;
  private final int end;

  private Range(int begin, int end) {
    this.begin = begin;
    this.end = end;
  }

  public int begin() {
    return begin;
  }

  public int end() {
    return end;
  }

  public boolean contains(int value) {
    return map2(
        requireGreaterThanOrEqual(value, begin),
        requireLowerThan(value, end), Tuple::of).isValid();
  }

  public int size() {
    return end - begin;
  }

  public Sequence<Integer> collect() {
    return map(identity());
  }

  public <T> Sequence<T> map(Function1<Integer, T> map) {
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
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(begin, end);
  }

  @Override
  public String toString() {
    return String.format("Range(%d..%d)", begin, end);
  }

  public static Range of(int begin, int end) {
    return map2(
        requireLowerThanOrEqual(begin, end),
        requireGreaterThanOrEqual(end, begin), Range::new).getOrElseThrow();
  }
}
