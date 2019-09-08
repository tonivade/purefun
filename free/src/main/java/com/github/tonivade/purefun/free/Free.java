/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Transformer;

@HigherKind
public interface Free<F extends Kind, A> {

  static <F extends Kind, T> Free<F, T> pure(T value) {
    return new Pure<>(value);
  }

  static <F extends Kind, T> Free<F, T> suspend(Higher1<F, Free<F, T>> value) {
    return new Suspend<>(value);
  }

  static <F extends Kind, T> Free<F, T> liftF(Functor<F> functor, Higher1<F, T> value) {
    return suspend(functor.map(value, Free::pure));
  }

  default <R> Free<F, R> map(Function1<A, R> map) {
    return flatMap(map.andThen(Free::pure));
  }

  <R> Free<F, R> flatMap(Function1<A, Free<F, R>> map);

  default <R> Free<F, R> andThen(Free<F, R> next) {
    return flatMap(ignore -> next);
  }

  Either<Higher1<F, Free<F, A>>, A> resume(Functor<F> functor);

  default <G extends Kind> Higher1<G, A> foldMap(Monad<G> monad,
                                                 Functor<F> functor,
                                                 Transformer<F, G> interpreter) {
    return resume(functor).fold(left -> FreeModule.suspend(monad, functor, interpreter, left), monad::<A>pure);
  }

  FreeModule module();

  final class Pure<F extends Kind, A> implements Free<F, A> {

    private final A value;

    private Pure(A value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<A, Free<F, B>> map) {
      return new FlatMapped<>(this, map);
    }

    @Override
    public Either<Higher1<F, Free<F, A>>, A> resume(Functor<F> functor) {
      return Either.right(value);
    }

    @Override
    public FreeModule module() {
      throw new UnsupportedOperationException();
    }
  }

  final class Suspend<F extends Kind, A> implements Free<F, A> {

    private final Higher1<F, Free<F, A>> value;

    private Suspend(Higher1<F, Free<F, A>> value) {
      this.value = requireNonNull(value);
    }

    @Override
    public <B> Free<F, B> flatMap(Function1<A, Free<F, B>> map) {
      return new FlatMapped<>(this, map);
    }

    @Override
    public Either<Higher1<F, Free<F, A>>, A> resume(Functor<F> functor) {
      return Either.left(value);
    }

    @Override
    public FreeModule module() {
      throw new UnsupportedOperationException();
    }
  }

  final class FlatMapped<F extends Kind, X, A, B> implements Free<F, B> {

    private final Free<F, A> value;
    private final Function1<A, Free<F, B>> next;

    private FlatMapped(Free<F, A> value, Function1<A, Free<F, B>> next) {
      this.value = requireNonNull(value);
      this.next = requireNonNull(next);
    }

    @Override
    public <C> Free<F, C> flatMap(Function1<B, Free<F, C>> map) {
      return new FlatMapped<>(value, free -> new FlatMapped<>(next.apply(free), map));
    }

    @Override
    public Either<Higher1<F, Free<F, B>>, B> resume(Functor<F> functor) {
      if (value instanceof Free.Suspend) {
        Free.Suspend<F, A> suspend = (Free.Suspend<F, A>) value;
        return Either.left(functor.map(suspend.value, fa -> fa.flatMap(next)));
      }
      if (value instanceof Free.Pure) {
        Free.Pure<F, A> pure = (Free.Pure<F, A>) value;
        return next.apply(pure.value).resume(functor);
      }
      Free.FlatMapped<F, ?, X, A> flatMapped = (Free.FlatMapped<F, ?, X, A>) value;
      return flatMapped.value.flatMap(x -> flatMapped.next.apply(x).flatMap(next)).resume(functor);
    }

    @Override
    public FreeModule module() {
      throw new UnsupportedOperationException();
    }
  }
}

interface FreeModule {

  static <A, F extends Kind, G extends Kind> Higher1<G, A> suspend(Monad<G> monad,
                                                                   Functor<F> functor,
                                                                   Transformer<F, G> interpreter,
                                                                   Higher1<F, Free<F, A>> left) {
    return monad.flatMap(interpreter.apply(left), free -> free.foldMap(monad, functor, interpreter));
  }
}