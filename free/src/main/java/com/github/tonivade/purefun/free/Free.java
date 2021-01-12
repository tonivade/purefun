/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.free.FreeOf.toFree;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.InjectK;
import com.github.tonivade.purefun.typeclasses.Monad;

@HigherKind
public abstract class Free<F extends Witness, A> implements FreeOf<F, A> {

  private Free() {}

  public static <F extends Witness, T> Free<F, T> pure(T value) {
    return new Pure<>(value);
  }

  public static <F extends Witness, T> Free<F, T> liftF(Kind<F, ? extends T> value) {
    return new Suspend<>(value);
  }

  public static <F extends Witness, G extends Witness, T> Free<G, T> inject(InjectK<F, G> inject, Kind<F, T> value) {
    return liftF(inject.inject(value));
  }

  public static <F extends Witness, T> Free<F, T> defer(Producer<? extends Free<F, ? extends T>> value) {
    Free<F, Unit> pure = pure(unit());
    return pure.flatMap(ignore -> value.get());
  }

  @SuppressWarnings("unchecked")
  public static <F extends Witness> Monad<Kind<Free_, F>> monadF() {
    return FreeMonad.INSTANCE;
  }

  public static <F extends Witness, G extends Witness> FunctionK<F, Kind<Free_, G>> functionKF(FunctionK<F, G> functionK) {
    return new FunctionK<F, Kind<Free_, G>>() {
      @Override
      public <T> Free<G, T> apply(Kind<F, ? extends T> from) {
        return liftF(functionK.apply(from));
      }
    };
  }

  public <R> Free<F, R> map(Function1<? super A, ? extends R> map) {
    return flatMap(map.andThen(Free::pure));
  }

  public abstract <R> Free<F, R> flatMap(Function1<? super A, ? extends Free<F, ? extends R>> map);

  public abstract Either<Kind<F, Free<F, A>>, A> resume(Functor<F> functor);

  public abstract Free<F, A> step();

  public <R> Free<F, R> andThen(Free<F, R> next) {
    return flatMap(ignore -> next);
  }

  public <G extends Witness> Kind<G, A> foldMap(Monad<G> monad, FunctionK<F, G> interpreter) {
    return monad.tailRecM(this, value -> value.step().foldStep(monad, interpreter));
  }

  protected abstract <G extends Witness> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter);

  public static final class Pure<F extends Witness, A> extends Free<F, A> {

    private final A value;

    private Pure(A value) {
      this.value = checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Free<F, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }

    @Override
    public Either<Kind<F, Free<F, A>>, A> resume(Functor<F> functor) {
      return Either.right(value);
    }

    @Override
    public Free<F, A> step() {
      return this;
    }

    @Override
    protected <G extends Witness> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      return monad.pure(Either.right(value));
    }
  }

  public static final class Suspend<F extends Witness, A> extends Free<F, A> {

    private final Kind<F, ? extends A> value;

    private Suspend(Kind<F, ? extends A> value) {
      this.value = checkNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<? super A, ? extends Free<F, ? extends B>> map) {
      return new FlatMapped<>(this, map);
    }

    @Override
    public Either<Kind<F, Free<F, A>>, A> resume(Functor<F> functor) {
      return Either.left(functor.map(value, Free::pure));
    }

    @Override
    public Free<F, A> step() {
      return this;
    }

    @Override
    protected <G extends Witness> Kind<G, Either<Free<F, A>, A>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      return monad.map(interpreter.apply(value), Either::right);
    }
  }

  public static final class FlatMapped<F extends Witness, X, A, B> extends Free<F, B> {

    private final Free<F, ? extends A> value;
    private final Function1<? super A, ? extends Free<F, ? extends B>> next;

    private FlatMapped(Free<F, ? extends A> value, Function1<? super A, ? extends Free<F, ? extends B>> next) {
      this.value = checkNonNull(value);
      this.next = checkNonNull(next);
    }

    @Override
    public <C> Free<F, C> flatMap(Function1<? super B, ? extends Free<F, ? extends C>> map) {
      return new FlatMapped<>(value, free -> new FlatMapped<>(next.apply(free), map));
    }

    @SuppressWarnings("unchecked")
    @Override
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
      return flatMapped.value.flatMap(x -> flatMapped.next.apply(x).flatMap(next)).resume(functor);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Free<F, B> step() {
      if (value instanceof FlatMapped) {
        Free.FlatMapped<F, ?, X, A> flatMapped = (Free.FlatMapped<F, ?, X, A>) value;
        return flatMapped.value.flatMap(x -> flatMapped.next.apply(x).flatMap(next)).step();
      }
      if (value instanceof Pure) {
        Free.Pure<F, A> pure = (Free.Pure<F, A>) value;
        Function1<? super A, Free<F, B>> andThen = next.andThen(FreeOf::narrowK);
        return andThen.apply(pure.value).step();
      }
      return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected <G extends Witness> Kind<G, Either<Free<F, B>, B>> foldStep(Monad<G> monad, FunctionK<F, G> interpreter) {
      Kind<G, A> foldMap = (Kind<G, A>) value.foldMap(monad, interpreter);
      Function1<? super A, Free<F, B>> andThen = next.andThen(FreeOf::narrowK);
      return monad.map(foldMap, andThen.andThen(Either::left));
    }
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
