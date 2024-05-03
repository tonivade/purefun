/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;
import static com.github.tonivade.purefun.core.Unit.unit;

import com.github.tonivade.purefun.Fixer;
import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;

@SuppressWarnings("unused")
public sealed interface For<F> {

  static <F> FlatMap<F> with(Monad<F> monad) {
    return new FlatMap<>(monad);
  }

  static <F> Apply<F> with(Applicative<F> applicative) {
    return new Apply<>(applicative);
  }

  static <F, T> FlatMap1<F, T> with(Monad<F> monad, Kind<F, ? extends T> value1) {
    return new FlatMap1<>(monad, cons(value1));
  }

  static <F, T> Apply1<F, T> with(Applicative<F> applicative, Kind<F, ? extends T> value1) {
    return new Apply1<>(applicative, cons(value1));
  }

  record FlatMap<F>(Monad<F> monad) implements For<F> {

    public <T> FlatMap1<F, T> and(T next) {
      return For.with(monad, monad.pure(next));
    }

    public <T> FlatMap1<F, T> then(Kind<F, T> next) {
      return For.with(monad, next);
    }

    public <T> FlatMap1<F, T> andThen(Producer<? extends Kind<F, ? extends T>> next) {
      return For.with(monad, monad.andThen(monad.pure(unit()), next));
    }
  }

  record Apply<F>(Applicative<F> applicative) implements For<F> {

    public <T> Apply1<F, T> and(T next) {
      return For.with(applicative, applicative.pure(next));
    }

    public <T> Apply1<F, T> then(Kind<F, T> next) {
      return For.with(applicative, next);
    }
 }
}

abstract class AbstractFlatMap<F, A, B> {

  protected final Monad<F> monad;
  protected final Function1<? super A, ? extends Kind<F, ? extends B>> value;

  protected AbstractFlatMap(Monad<F> monad, Function1<? super A, ? extends Kind<F, ? extends B>> value) {
    this.monad = checkNonNull(monad);
    this.value = checkNonNull(value);
  }

  public abstract Kind<F, B> run();

  public <R> R fix(Fixer<Kind<F, B>, R> fixer) {
    return run().fix(fixer);
  }

  public void end(Consumer1<? super Kind<F, B>> consumer) {
    consumer.accept(run());
  }

  public <R> Kind<F, R> returns(R other) {
    return monad.map(run(), ignore -> other);
  }
}

abstract class AbstractApply<F, A> {

  protected final Applicative<F> applicative;
  protected final Producer<? extends Kind<F, ? extends A>> value;

  protected AbstractApply(Applicative<F> applicative, Producer<? extends Kind<F, ? extends A>> value) {
    this.applicative = checkNonNull(applicative);
    this.value = checkNonNull(value);
  }

  public abstract Kind<F, A> run();

  public <R> R fix(Fixer<Kind<F, A>, R> fixer) {
    return run().fix(fixer);
  }

  public void end(Consumer1<? super Kind<F, A>> consumer) {
    consumer.accept(run());
  }

  public <R> Kind<F, R> returns(R other) {
    return applicative.map(run(), ignore -> other);
  }
}
