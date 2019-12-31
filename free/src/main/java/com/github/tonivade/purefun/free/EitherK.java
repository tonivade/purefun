/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Either;

import static java.util.Objects.requireNonNull;

@HigherKind
public final class EitherK<F extends Kind, G extends Kind, T> {

  private final Either<Higher1<F, T>, Higher1<G, T>> either;

  private EitherK(Either<Higher1<F, T>, Higher1<G, T>> either) {
    this.either = requireNonNull(either);
  }

  public <R> R fold(Function1<Higher1<F, T>, R> left, Function1<Higher1<G, T>, R> right) {
    return either.fold(left, right);
  }

  public Higher1<F, T> getLeft() {
    return either.getLeft();
  }

  public Higher1<G, T> getRight() {
    return either.getRight();
  }

  public static <F extends Kind, G extends Kind, T> EitherK<F, G, T> left(Higher1<F, T> left) {
    return new EitherK<>(Either.left(left));
  }

  public static <F extends Kind, G extends Kind, T> EitherK<F, G, T> right(Higher1<G, T> right) {
    return new EitherK<>(Either.right(right));
  }
}
