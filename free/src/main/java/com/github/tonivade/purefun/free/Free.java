/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.free.FreeOf.toFree;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Bindable;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Functor;
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
    return pure.flatMap(ignore -> value.get());
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness> Monad<Kind<Free_, F>> monadF() {
    return FreeMonad.INSTANCE;
  }

  public static <F extends Witness, G extends Witness> FunctionK<F, Kind<Free_, G>> functionKF(FunctionK<F, G> functionK) {
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
    return monad.tailRecM(this, value -> value.step().foldStep(monad, interpreter));
  }

  final class Pure<F extends Witness, A> implements Free<F, A> {

    private final A value;

    private Pure(A value) {
      this.value = checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }

    public Either<Kind<F, Free<F, A>>, A> resume(Functor<F> functor) {
      return Either.right(value);
    }

    public Free<F, A> step() {
      return this;
    }

    protected <G extends Witness> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      return monad.pure(Either.right(value));
    }
  }

  final class Suspend<F extends Witness, A> implements Free<F, A> {

    private final Kind<F, ? extends A> value;

    private Suspend(Kind<F, ? extends A> value) {
      this.value = checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }

    public Either<Kind<F, Free<F, A>>, A> resume(Functor<F> functor) {
      return Either.left(functor.map(value, Free::pure));
    }

    public Free<F, A> step() {
      return this;
    }

    protected <G extends Witness> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      return monad.map(interpreter.apply(value), Either::right);
    }
  }

  final class FlatMapped<F extends Witness, X, A, B> implements Free<F, B> {

    private final Free<F, ? extends A> value;
    private final Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends B>> next;

    private FlatMapped(Free<F, ? extends A> value, Function1<? super A, ? extends Kind<Kind<Free_, F>, ? extends B>> next) {
      this.value = checkNonNull(value);
      this.next = checkNonNull(next);
    }

    @Override
    public <C> Free<F, C> flatMap(Function1<? super B, ? extends Kind<Kind<Free_, F>, ? extends C>> map) {
      return new FlatMapped<>(value, free -> new FlatMapped<>(next.andThen(FreeOf::<F, B>narrowK).apply(free), map));
    }

    @SuppressWarnings("unchecked")
    public Either<Kind<F, Free<F, B>>, B> resume(Functor<F> functor) {
      if (value instanceof Free.Suspend) {
        Free.Suspend<F, A> suspend = (Free.Suspend<F, A>) value;
        Kind<F, Free<F, B>> map = functor.map(suspend.value, next.andThen(x -> (Free<F, B>) x));
        return Either.left(map);
      }
      if (value instanceof Free.Pure) {
        Free.Pure<F, A> pure = (Free.Pure<F, A>) value;
        Free<F, B> apply = (Free<F, B>) next.apply(pure.value);
        return apply.resume(functor);
      }
      Free.FlatMapped<F, ?, X, A> flatMapped = (Free.FlatMapped<F, ?, X, A>) value;
      return flatMapped.value.flatMap(x -> flatMapped.next.andThen(FreeOf::<F, A>narrowK).apply(x).flatMap(next)).resume(functor);
    }

    @SuppressWarnings("unchecked")
    public Free<F, B> step() {
      if (value instanceof FlatMapped) {
        Free.FlatMapped<F, ?, X, A> flatMapped = (Free.FlatMapped<F, ?, X, A>) value;
        return flatMapped.value.flatMap(x -> flatMapped.next.andThen(FreeOf::<F, A>narrowK).apply(x).flatMap(next)).step();
      }
      if (value instanceof Pure) {
        Free.Pure<F, A> pure = (Free.Pure<F, A>) value;
        Function1<? super A, Free<F, B>> andThen = next.andThen(FreeOf::narrowK);
        return andThen.apply(pure.value).step();
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    protected <G extends Witness> Kind<G, Either<Free<F, B>, B>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      Kind<G, A> foldMap = (Kind<G, A>) value.foldMap(monad, interpreter);
      Function1<? super A, Free<F, B>> andThen = next.andThen(FreeOf::narrowK);
      return monad.map(foldMap, andThen.andThen(Either::left));
    }
  }
  
  private Free<F, A> step() {
    if (this instanceof Pure<F, A> pure) {
      return pure.step();
    }
    if (this instanceof Suspend<F, A> suspend) {
      return suspend.step();
    }
    var flatMapped = (Free.FlatMapped<F, ?, ?, A>) this;
    return flatMapped.step();
  }
  
  private <G extends Witness> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
    if (this instanceof Pure<F, A> pure) {
      return pure.foldStep(monad, interpreter);
    }
    if (this instanceof Suspend<F, A> suspend) {
      return suspend.foldStep(monad, interpreter);
    }
    var flatMapped = (Free.FlatMapped<F, ?, ?, A>) this;
    return flatMapped.foldStep(monad, interpreter);
  }
  
  private Either<Kind<F, Free<F, A>>, A> resume(Functor<F> functor) {
    if (this instanceof Pure<F, A> pure) {
      return pure.resume(functor);
    }
    if (this instanceof Suspend<F, A> suspend) {
      return suspend.resume(functor);
    }
    var flatMapped = (Free.FlatMapped<F, ?, ?, A>) this;
    return flatMapped.resume(functor);
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
