/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.free.FreeOf.toFree;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.InjectK;
import com.github.tonivade.purefun.typeclasses.Monad;

@HigherKind
public sealed interface Free<F extends Witness, A> extends FreeOf<F, A>, Bindable<Kind<Free_, F>, A> {

  static <F extends Witness, T> Free<F, T> pure(T value) {
    return new Pure<>(value);
  }

  static <F extends Witness, T> Free<F, T> liftF(Kind<F, ? extends T> value) {
    return new Suspend<>(value);
  }

  static <F extends Witness, G extends Witness, T> Free<G, T> inject(InjectK<F, G> inject, Kind<F, T> value) {
    return liftF(inject.inject(value));
  }

  static <F extends Witness, T> Free<F, T> defer(Producer<? extends Free<F, ? extends T>> value) {
    Free<F, Unit> pure = pure(unit());
    return pure.flatMap(value.asFunction());
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness> Monad<Kind<Free_, F>> monadF() {
    return FreeMonad.INSTANCE;
  }

  static <F extends Witness, G extends Witness> FunctionK<F, Kind<Free_, G>> functionKF(FunctionK<F, G> functionK) {
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
  <R> Free<F, R> flatMap(Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends R>> mapper);

  @Override
  default <R> Free<F, R> andThen(Kind<Kind<Free_, F>, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  default <G extends Witness> Kind<G, A> foldMap(Monad<G> monad, FunctionK<F, G> interpreter) {
    return monad.tailRecM(this, value -> value.foldStep(monad, interpreter));
  }

  record Pure<F extends Witness, A>(A value) implements Free<F, A> {

    public Pure {
      checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }
  }

  record Suspend<F extends Witness, A>(Kind<F, ? extends A> value) implements Free<F, A> {

    public Suspend {
      checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }
  }

  record FlatMapped<F extends Witness, A, B>(Free<F, ? extends A> value, Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends B>> next) implements Free<F, B> {

    public FlatMapped {
      checkNonNull(value);
      checkNonNull(next);
    }

    @Override
    public <C> Free<F, C> flatMap(Function1<? super B, ? extends Kind<Kind<Free_, F>, ? extends C>> map) {
      return new FlatMapped<>(value, free -> new FlatMapped<>(next.andThen(FreeOf::<F, B>narrowK).apply(free), map));
    }

    private <G extends Witness> Kind<G, Either<Free<F, B>, B>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      return monad.map(value.foldMap(monad, interpreter), next.andThen(FreeOf::narrowK).andThen(Either::left));
    }
  }

  private <G extends Witness> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
    return switch (this) {
      case Pure<F, A>(var value) -> monad.pure(Either.right(value));
      case Suspend<F, A>(var value) -> monad.map(interpreter.apply(value), Either::right);
      case Free.FlatMapped<F, ?, A> flatMapped -> flatMapped.foldStep(monad, interpreter);
    };
  }
}

interface FreeMonad<F extends Witness> extends Monad<Kind<Free_, F>> {

  @SuppressWarnings("rawtypes")
  FreeMonad INSTANCE = new FreeMonad() {};

  @Override
  default <T> Free<F, T> pure(T value) {
    return Free.pure(value);
  }

  @Override
  default <T, R> Free<F, R> flatMap(
      Kind<Kind<Free_, F>, ? extends T> value, Function1<? super T, ? extends Kind<Kind<Free_, F>, ? extends R>> map) {
    return value.fix(toFree()).flatMap(map.andThen(FreeOf::narrowK));
  }
}
