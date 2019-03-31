/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Transformer;

public interface Free<F extends Kind, T> extends FlatMap2<Free.µ, F, T> {

  final class µ implements Kind {}

  static <F extends Kind, T> Free<F, T> pure(T value) {
    return new Pure<>(value);
  }

  static <F extends Kind, T> Free<F, T> suspend(Higher1<F, Free<F, T>> value) {
    return new Suspend<>(value);
  }

  static <F extends Kind, T> Free<F, T> liftF(Functor<F> functor, Higher1<F, T> value) {
    return suspend(functor.map(value, Free::pure));
  }

  static <F extends Kind, T> Free<F, T> narrowK(Higher2<Free.µ, F, T> hkt) {
    return (Free<F, T>) hkt;
  }

  static <F extends Kind, T> Free<F, T> narrowK(Higher1<Higher1<Free.µ, F>, T> hkt) {
    return (Free<F, T>) hkt;
  }

  @Override
  default <R> Free<F, R> map(Function1<T, R> map) {
    return flatMap(map.andThen(Free::pure));
  }

  @Override
  <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<Free.µ, F, R>> map);

  default <R> Free<F, R> andThen(Free<F, R> next) {
    return flatMap(ignore -> next);
  }

  default Either<Higher1<F, Free<F, T>>, T> resume(Functor<F> functor) {
    return FreeModule.resume(this, functor);
  }

  default <G extends Kind> Higher1<G, T> foldMap(Monad<G> monad,
                                                 Functor<F> functor,
                                                 Transformer<F, G> interpreter) {
    return resume(functor).fold(left -> FreeModule.suspend(monad, functor, interpreter, left), monad::pure);
  }

  FreeModule module();

  final class Pure<F extends Kind, T> implements Free<F, T> {

    final T value;

    private Pure(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<Free.µ, F, R>> map) {
      return new FlatMapped<>(this, map);
    }

    @Override
    public FreeModule module() {
      throw new UnsupportedOperationException();
    }
  }

  final class Suspend<F extends Kind, T> implements Free<F, T> {

    final Higher1<F, Free<F, T>> value;

    private Suspend(Higher1<F, Free<F, T>> value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <R> Free<F, R> flatMap(Function1<T, ? extends Higher2<Free.µ, F, R>> map) {
      return new FlatMapped<>(this, map);
    }

    @Override
    public FreeModule module() {
      throw new UnsupportedOperationException();
    }
  }

  final class FlatMapped<F extends Kind, T, R> implements Free<F, R> {

    final Higher2<Free.µ, F, T> value;
    final Function1<T, ? extends Higher2<Free.µ, F, R>> map;

    private FlatMapped(Higher2<Free.µ, F, T> value, Function1<T, ? extends Higher2<Free.µ, F, R>> map) {
      this.value = requireNonNull(value);
      this.map = requireNonNull(map);
    }

    @Override
    public <X> Free<F, X> flatMap(Function1<R, ? extends Higher2<Free.µ, F, X>> map) {
      return new FlatMapped<>(value, free -> new FlatMapped<>(narrowFn().apply(free), map));
    }

    Function1<T, Free<F, R>> narrowFn() {
      return map.andThen(Free::narrowK);
    }

    Free<F, T> narrowK() {
      return Free.narrowK(value);
    }

    @Override
    public FreeModule module() {
      throw new UnsupportedOperationException();
    }
  }
}

interface FreeModule {

  static <F extends Kind, T> Free.Pure<F, T> asPure(Free<F, T> free) {
    return (Free.Pure<F, T>) free;
  }

  static <F extends Kind, T> Free.Suspend<F, T> asSuspend(Free<F, T> free) {
    return (Free.Suspend<F, T>) free;
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind, T, X> Free.FlatMapped<F, X, T> asFlatMapped(Free<F, T> free) {
    return (Free.FlatMapped<F, X, T>) free;
  }

  static <X1, X2, F extends Kind, T> Either<Higher1<F, Free<F, T>>, T> resume(Free<F, T> current, Functor<F> functor) {
    while (true) {
      if (current instanceof Free.Suspend) {
        return Either.left(asSuspend(current).value);
      } else if (current instanceof Free.Pure) {
        return Either.right(asPure(current).value);
      }
      Free.FlatMapped<F, X1, T> flatMap1 = asFlatMapped(current);
      Free<F, X1> innerFree1 = flatMap1.narrowK();
      if (innerFree1 instanceof Free.Suspend) {
        return Either.left(functor.map(asSuspend(innerFree1).value,
                                       x1 -> x1.flatMap(flatMap1.map)));
      }
      if (innerFree1 instanceof Free.Pure) {
        current = flatMap1.narrowFn().apply(asPure(innerFree1).value);
      } else {
        Free.FlatMapped<F, X2, X1> flatMap2 = asFlatMapped(innerFree1);
        Free<F, X2> innerValue2 = flatMap2.narrowK();
        current = innerValue2.flatMap(x2 -> flatMap2.narrowFn().apply(x2).flatMap(flatMap1.map));
      }
    }
  }

  static <T, F extends Kind, G extends Kind> Higher1<G, T> suspend(Monad<G> monad,
                                                                   Functor<F> functor,
                                                                   Transformer<F, G> interpreter,
                                                                   Higher1<F, Free<F, T>> left) {
    return monad.flatMap(interpreter.apply(left), free -> free.foldMap(monad, functor, interpreter));
  }
}