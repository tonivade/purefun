/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import java.io.Serializable;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;

/**
 * <p>This is the identity monad. It only wraps the value and nothing more.</p>
 * <p>You can go from {@code T} to {@code Id<T>} and from {@code Id<T>} to {@code T}
 * without loosing information.</p>
 * @param <T> the wrapped value
 */
@HigherKind
public record Id<T>(T value) implements IdOf<T>, Bindable<Id_, T>, Serializable {

  public Id {
    checkNonNull(value);
  }

  @Override
  public <R> Id<R> map(Function1<? super T, ? extends R> map) {
    return flatMap(map.andThen(Id::of));
  }

  @Override
  public <R> Id<R> flatMap(Function1<? super T, ? extends Kind<Id_, ? extends R>> map) {
    return map.andThen(IdOf::<R>narrowK).apply(value);
  }

  @Override
  public String toString() {
    return "Id(" + value + ")";
  }

  public static <T> Id<T> of(T value) {
    return new Id<>(value);
  }
}