/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class Equal<T> {

  private final T target;
  private final List<Tester<T>> testers = new LinkedList<>();

  private Equal(T target) {
    this.target = requireNonNull(target);
  }

  public Equal<T> append(Tester<T> tester) {
    this.testers.add(requireNonNull(tester));
    return this;
  }

  @SuppressWarnings("unchecked")
  public boolean applyTo(Object obj) {
    if (isNull(obj)) {
      return false;
    }
    if (sameObjects(obj)) {
      return true;
    }
    return sameClasses(obj) && areEquals((T) obj);
  }

  private boolean areEquals(T other) {
    return testers.stream().allMatch(tester -> tester.apply(target, other));
  }

  private boolean sameClasses(Object obj) {
    return target.getClass() == obj.getClass();
  }

  private boolean sameObjects(Object obj) {
    return target == obj;
  }

  public static <T> Equal<T> of(T target) {
    return new Equal<>(target);
  }

  @FunctionalInterface
  public interface Tester<T> extends Function2<T, T, Boolean> {
  }

  public static <T, V> Tester<T> comparing(Function1<T, V> getter) {
    return (a, b) -> Objects.equals(getter.apply(a), getter.apply(b));
  }

  public static <T, V> Tester<T> comparingArray(Function1<T, V[]> getter) {
    return (a, b) -> Arrays.deepEquals(getter.apply(a), getter.apply(b));
  }
}
