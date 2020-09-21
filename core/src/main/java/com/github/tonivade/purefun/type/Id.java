/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import java.io.Serializable;
import java.util.Objects;
import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;

/**
 * <p>This is the identity monad. It only wraps the value and nothing more.</p>
 * <p>You can go from {@code T} to {@code Id<T>} and from {@code Id<T>} to {@code T}
 * without loosing information.</p>
 * @param <T> the wrapped value
 */
@HigherKind
public final class Id<T> implements IdOf<T>, Serializable {

  private static final long serialVersionUID = -6295106408421985189L;

  private static final Equal<Id<?>> EQUAL = Equal.<Id<?>>of().comparing(Id::get);

  private final T value;

  private Id(T value) {
    this.value = checkNonNull(value);
  }

  public <R> Id<R> map(Function1<? super T, ? extends R> map) {
    return flatMap(map.andThen(Id::of));
  }

  public <R> Id<R> flatMap(Function1<? super T, ? extends Id<? extends R>> map) {
    return map.andThen(IdOf::<R>narrowK).apply(value);
  }

  public T get() {
    return value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    return EQUAL.applyTo(this, obj);
  }

  @Override
  public String toString() {
    return "Id(" + value + ")";
  }

  public static <T> Id<T> of(T value) {
    return new Id<>(value);
  }
}