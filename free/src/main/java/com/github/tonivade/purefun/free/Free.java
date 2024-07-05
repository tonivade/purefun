/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Unit.unit;
import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.InjectK;
import com.github.tonivade.purefun.typeclasses.Monad;

public sealed interface Free<F extends Kind<F, ?>, A> extends Kind<Free<F, ?>, A>, Bindable<Free<F, ?>, A> {

  static <F extends Kind<F, ?>, T> Free<F, T> pure(T value) {
    return new Pure<>(value);
  }

  static <F extends Kind<F, ?>, T> Free<F, T> liftF(Kind<F, ? extends T> value) {
    return new Suspend<>(value);
  }

  static <F extends Kind<F, ?>, G extends Kind<G, ?>, T> Free<G, T> inject(InjectK<F, G> inject, Kind<F, T> value) {
    return liftF(inject.inject(value));
  }

  static <F extends Kind<F, ?>, T> Free<F, T> defer(Producer<? extends Free<F, ? extends T>> value) {
    Free<F, Unit> pure = pure(unit());
    return pure.flatMap(value.asFunction());
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>> Monad<Free<F, ?>> monadF() {
    return FreeMonad.INSTANCE;
  }

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> FunctionK<F, Free<G, ?>> functionKF(FunctionK<F, G> functionK) {
    return new FunctionK<>() {
      @Override
      public <T> Free<G, T> apply(Kind<F, ? extends T> from) {
        return liftF(functionK.apply(from));
      }
    };
  }

  @Override
  default <R> Free<F, R> map(Function1<? super A, ? extends R> map) {
    return flatMap(map.andThen(Free::pure));
  }

  @Override
  <R> Free<F, R> flatMap(Function1<? super A, ? extends Kind<Free<F, ?>, ? extends R>> mapper);

  @Override
  default <R> Free<F, R> andThen(Kind<Free<F, ?>, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  default <G extends Kind<G, ?>> Kind<G, A> foldMap(Monad<G> monad, FunctionK<F, G> interpreter) {
    return monad.tailRecM(this, value -> value.foldStep(monad, interpreter));
  }

  record Pure<F extends Kind<F, ?>, A>(A value) implements Free<F, A> {

    public Pure {
      checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Kind<Free<F, ?>, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }
  }

  record Suspend<F extends Kind<F, ?>, A>(Kind<F, ? extends A> value) implements Free<F, A> {

    public Suspend {
      checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Kind<Free<F, ?>, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }
  }

  record FlatMapped<F extends Kind<F, ?>, A, B>(Free<F, ? extends A> value,
      Function1<? super A, ? extends Kind<Free<F, ?>, ? extends B>> next) implements Free<F, B> {

    public FlatMapped {
      checkNonNull(value);
      checkNonNull(next);
    }

    @Override
    public <C> Free<F, C> flatMap(Function1<? super B, ? extends Kind<Free<F, ?>, ? extends C>> map) {
      return new FlatMapped<>(value, free -> new FlatMapped<>(next.apply(free).fix(), map));
    }

    private <G extends Kind<G, ?>> Kind<G, Either<Free<F, B>, B>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      Kind<G, ? extends A> foldMap = value.foldMap(monad, interpreter);
      Function1<? super A, Free<F, B>> andThen = next.andThen(Kind::<Free<F, B>>fix);
      return monad.map(foldMap, andThen.andThen(Either::left));
    }
  }

  private <G extends Kind<G, ?>> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
    return switch (this) {
      case Pure<F, A>(var value) -> monad.pure(Either.right(value));
      case Suspend<F, A>(var value) -> monad.map(interpreter.apply(value), Either::right);
      case FlatMapped<F, ?, A> flatMapped -> flatMapped.foldStep(monad, interpreter);
    };
  }
}

interface FreeMonad<F extends Kind<F, ?>> extends Monad<Free<F, ?>> {

  @SuppressWarnings("rawtypes")
  FreeMonad INSTANCE = new FreeMonad() {};

  @Override
  default <T> Free<F, T> pure(T value) {
    return Free.pure(value);
  }

  @Override
  default <T, R> Free<F, R> flatMap(
      Kind<Free<F, ?>, ? extends T> value, Function1<? super T, ? extends Kind<Free<F, ?>, ? extends R>> map) {
    return value.<Free<F, T>>fix().flatMap(map).fix();
  }
}
