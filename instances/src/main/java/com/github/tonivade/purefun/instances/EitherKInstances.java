/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.free.EitherK;
import com.github.tonivade.purefun.free.EitherK_;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.InjectK;

public interface EitherKInstances {

  static <F extends Kind, G extends Kind, T> Eq<Higher1<Higher1<Higher1<EitherK_, F>, G>, T>> eq(
      Eq<Higher1<F, T>> leftEq, Eq<Higher1<G, T>> rightEq) {
    return (a, b) -> Pattern2.<EitherK<F, G, T>, EitherK<F, G, T>, Boolean>build()
        .when((x, y) -> x.isLeft() && y.isLeft())
          .then((x, y) -> leftEq.eqv(x.getLeft(), y.getLeft()))
        .when((x, y) -> x.isRight() && y.isRight())
          .then((x, y) -> rightEq.eqv(x.getRight(), y.getRight()))
        .otherwise()
          .returns(false)
        .apply(EitherK_.narrowK(a), EitherK_.narrowK(b));
  }

  static <F extends Kind, G extends Kind> Functor<Higher1<Higher1<EitherK_, F>, G>> functor(
      Functor<F> functorF, Functor<G> functorG) {
    return EitherKFunctor.instance(checkNonNull(functorF), checkNonNull(functorG));
  }

  static <F extends Kind, G extends Kind> Contravariant<Higher1<Higher1<EitherK_, F>, G>> contravariant(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return EitherKContravariant.instance(checkNonNull(contravariantF), checkNonNull(contravariantG));
  }

  static <F extends Kind, G extends Kind> Comonad<Higher1<Higher1<EitherK_, F>, G>> comonad(
      Comonad<F> comonadF, Comonad<G> comonadG) {
    return EitherKComonad.instance(checkNonNull(comonadF), checkNonNull(comonadG));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind, G extends Kind> InjectK<F, Higher1<Higher1<EitherK_, F>, G>> injectEitherKLeft() {
    return EitherKInjectKLeft.INSTANCE;
  }

  static <F extends Kind, R extends Kind, G extends Kind>
      InjectK<F, Higher1<Higher1<EitherK_, G>, R>> injectEitherKRight(InjectK<F, R> inject) {
    return EitherKInjectKRight.instance(checkNonNull(inject));
  }
}

interface EitherKFunctor<F extends Kind, G extends Kind> extends Functor<Higher1<Higher1<EitherK_, F>, G>> {

  static <F extends Kind, G extends Kind> EitherKFunctor<F, G> instance(Functor<F> functorF, Functor<G> functorG) {
    return new EitherKFunctor<F, G>() {
      @Override
      public Functor<F> f() { return functorF; }
      @Override
      public Functor<G> g() { return functorG; }
    };
  }

  Functor<F> f();
  Functor<G> g();

  @Override
  default <T, R> Higher3<EitherK_, F, G, R> map(
      Higher1<Higher1<Higher1<EitherK_, F>, G>, T> value, Function1<T, R> map) {
    return value.fix1(EitherK_::narrowK).map(f(), g(), map);
  }
}

interface EitherKContravariant<F extends Kind, G extends Kind>
    extends Contravariant<Higher1<Higher1<EitherK_, F>, G>> {

  static <F extends Kind, G extends Kind> EitherKContravariant<F, G> instance(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return new EitherKContravariant<F, G>() {
      @Override
      public Contravariant<F> f() { return contravariantF; }
      @Override
      public Contravariant<G> g() { return contravariantG; }
    };
  }

  Contravariant<F> f();
  Contravariant<G> g();

  @Override
  default <A, B> Higher3<EitherK_, F, G, B> contramap(
      Higher1<Higher1<Higher1<EitherK_, F>, G>, A> value, Function1<B, A> map) {
    return value.fix1(EitherK_::narrowK).contramap(f(), g(), map);
  }
}

interface EitherKComonad<F extends Kind, G extends Kind>
    extends Comonad<Higher1<Higher1<EitherK_, F>, G>>, EitherKFunctor<F, G> {

  static <F extends Kind, G extends Kind> EitherKComonad<F, G> instance(Comonad<F> comonadF, Comonad<G> comonadG) {
    return new EitherKComonad<F, G>() {
      @Override
      public Comonad<F> f() { return comonadF; }
      @Override
      public Comonad<G> g() { return comonadG; }
    };
  }

  @Override
  Comonad<F> f();
  @Override
  Comonad<G> g();

  @Override
  default <A, B> Higher3<EitherK_, F, G, B> coflatMap(
      Higher1<Higher1<Higher1<EitherK_, F>, G>, A> value,
      Function1<Higher1<Higher1<Higher1<EitherK_, F>, G>, A>, B> map) {
    return value.fix1(EitherK_::narrowK).coflatMap(f(), g(), eitherK -> map.apply(eitherK));
  }

  @Override
  default <A> A extract(Higher1<Higher1<Higher1<EitherK_, F>, G>, A> value) {
    return value.fix1(EitherK_::narrowK).extract(f(), g());
  }
}

interface EitherKInjectKRight<F extends Kind, G extends Kind, R extends Kind>
    extends InjectK<F, Higher1<Higher1<EitherK_, G>, R>> {

  static <F extends Kind, X extends Kind, R extends Kind> EitherKInjectKRight<F, X, R> instance(InjectK<F, R> injectK) {
    return () -> injectK;
  }

  InjectK<F, R> inject();

  @Override
  default  <T> Higher3<EitherK_, G, R, T> inject(Higher1<F, T> value) {
    return EitherK.<G, R, T>right(inject().inject(value));
  }
}

interface EitherKInjectKLeft<F extends Kind, G extends Kind> extends InjectK<F, Higher1<Higher1<EitherK_, F>, G>> {

  @SuppressWarnings("rawtypes")
  EitherKInjectKLeft INSTANCE = new EitherKInjectKLeft() {};

  @Override
  default  <T> Higher3<EitherK_, F, G, T> inject(Higher1<F, T> value) {
    return EitherK.<F, G, T>left(value);
  }
}
