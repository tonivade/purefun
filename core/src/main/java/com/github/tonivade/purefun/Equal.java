/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

/**
 * This is a utility class to generate more readable {@code equals()} methods. It's based on {@link Eq} instances and it can combine
 * some of them to generate a bigger function that verify the equivalence between two instances of the same type {@code T}.
 *
 * <pre>{@code
 * @Override
 * public boolean equals(Object obj) {
 *   return Equal.of(this)
 *     .comparing(Data::getId)
 *     .comparing(Data::getValue)
 *     .applyTo(obj);
 * }
 * }</pre>
 * @param <T> type to which it applies
 */
public final class Equal<T> {

  private final T target;
  private final Eq<T> tester;

  private Equal(T target, Eq<T> tester) {
    this.target = requireNonNull(target);
    this.tester = requireNonNull(tester);
  }

  public Equal<T> append(Eq<T> other) {
    return new Equal<>(target, tester.and(other));
  }

  public <V> Equal<T> comparing(Function1<T, V> getter) {
    return append(Eq.comparing(getter));
  }

  public <V> Equal<T> comparingArray(Function1<T, V[]> getter) {
    return append(Eq.comparingArray(getter));
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
    return tester.eqv(target, other);
  }

  private boolean sameClasses(Object obj) {
    return target.getClass() == obj.getClass();
  }

  private boolean sameObjects(Object obj) {
    return target == obj;
  }

  public static <T> Equal<T> of(T target) {
    return new Equal<>(target, Eq.always());
  }
}
