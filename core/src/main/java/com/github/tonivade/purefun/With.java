/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.util.Objects;

public final class With<A> {

  private static final Equal<With<?>> EQUAL = Equal.<With<?>>of().comparing(With::get);

  private final A value;

  private With(A value) {
    this.value = checkNonNull(value);
  }

  public A get() {
    return value;
  }

  public void end(Consumer1<? super A> consumer) {
     consumer.accept(value);
  }

  public <R> With<R> then(Function1<? super A, ? extends R> function) {
    return with(function.apply(value));
  }

  public <B, R> With<R> then(Function2<? super A, ? super B, ? extends R> function, B b) {
    return with(function.apply(value, b));
  }

  public <B, C, R> With<R> then(Function3<? super A, ? super B, ? super C, ? extends R> function, B b, C c) {
    return with(function.apply(value, b, c));
  }

  public <B, C, D, R> With<R> then(Function4<? super A, ? super B, ? super C, ? super D, ? extends R> function, B b, C c, D d) {
    return with(function.apply(value, b, c, d));
  }

  public <B, C, D, E, R> With<R> then(Function5<? super A, ? super B, ? super C, ? super D, ? super E, ? extends R> function, B b, C c, D d, E e) {
    return with(function.apply(value, b, c, d, e));
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  public static <T> With<T> with(T value) {
    return new With<>(value);
  }
}
