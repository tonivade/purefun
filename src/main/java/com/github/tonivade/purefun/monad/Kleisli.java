/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Monad3;
import com.github.tonivade.purefun.algebra.Monad;

public final class Kleisli<F extends Kind, Z, A> implements Monad3<Kleisli.µ, F, Z, A> {

  public static final class µ implements Kind {}

  private final Monad<F> monad;
  private final Function1<Z, Higher1<F, A>> run;

  private Kleisli(Monad<F> monad, Function1<Z, Higher1<F, A>> run) {
    this.monad = requireNonNull(monad);
    this.run = requireNonNull(run);
  }

  public Higher1<F, A> run(Z value) {
    return run.apply(value);
  }

  @Override
  public <R> Kleisli<F, Z, R> map(Function1<A, R> map) {
    return Kleisli.of(monad, value -> monad.map(run(value), map));
  }

  @Override
  public <R> Kleisli<F, Z, R> flatMap(Function1<A, ? extends Higher3<Kleisli.µ, F, Z, R>> map) {
    return Kleisli.of(monad, value -> monad.flatMap(run(value), a -> map.andThen(Kleisli::narrowK).apply(a).run(value)));
  }

  public <B> Kleisli<F, Z, B> compose(Kleisli<F, A, B> other) {
    return Kleisli.of(monad, value -> monad.flatMap(run(value), a -> other.run(a)));
  }

  public <X> Kleisli<F, X, A> local(Function1<X, Z> map) {
    return Kleisli.of(monad, map.andThen(this::run)::apply);
  }

  public static <F extends Kind, A, B> Kleisli<F, A, B> lift(Monad<F> monad, Function1<A, B> map) {
    return Kleisli.of(monad, map.andThen(monad::pure)::apply);
  }

  public static <F extends Kind, A, B> Kleisli<F, A, B> of(Monad<F> monad, Function1<A, Higher1<F, B>> run) {
    return new Kleisli<>(monad, run);
  }

  public static <F extends Kind, A, B> Kleisli<F, A, B> narrowK(Higher3<Kleisli.µ, F, A, B> hkt) {
    return (Kleisli<F, A, B>) hkt;
  }

  public static <F extends Kind, A, B> Kleisli<F, A, B> narrowK(Higher2<Higher1<Kleisli.µ, F>, A, B> hkt) {
    return (Kleisli<F, A, B>) hkt;
  }

  public static <F extends Kind, A, B> Kleisli<F, A, B> narrowK(Higher1<Higher1<Higher1<Kleisli.µ, F>, A>, B> hkt) {
    return (Kleisli<F, A, B>) hkt;
  }
}