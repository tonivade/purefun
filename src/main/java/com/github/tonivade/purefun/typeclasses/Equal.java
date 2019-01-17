/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;

public final class Equal<T> {

  private final T target;
  private final Sequence<Eq<T>> testers;

  private Equal(T target) {
    this(target, ImmutableList.empty());
  }

  private Equal(T target, Sequence<Eq<T>> testers) {
    this.target = requireNonNull(target);
    this.testers = requireNonNull(testers);
  }

  public Equal<T> append(Eq<T> tester) {
    return new Equal<>(target, testers.append(tester));
  }

  public <V> Equal<T> comparing(Function1<T, V> getter) {
    return append(Eq.comparing(getter));
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
    return testers.stream().allMatch(tester -> tester.eqv(target, other));
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
}
