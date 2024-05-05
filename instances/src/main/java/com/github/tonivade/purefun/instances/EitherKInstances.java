/*
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.free.EitherK;
import com.github.tonivade.purefun.free.EitherKOf;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.InjectK;

public interface EitherKInstances {

  static <F, G, T> Eq<Kind<Kind<Kind<EitherK<?, ?, ?>, F>, G>, T>> eq(
      Eq<Kind<F, T>> leftEq, Eq<Kind<G, T>> rightEq) {
    return (a, b) -> {
      var x = EitherKOf.narrowK(a);
      var y = EitherKOf.narrowK(b);
      if (x.isLeft() && y.isLeft()) {
        return leftEq.eqv(x.getLeft(), y.getLeft());
      }
      if (x.isRight() && y.isRight()) {
        return rightEq.eqv(x.getRight(), y.getRight());
      }
      return false;
    };
  }

  static <F, G> Functor<Kind<Kind<EitherK<?, ?, ?>, F>, G>> functor(
      Functor<F> functorF, Functor<G> functorG) {
    return EitherKFunctor.instance(checkNonNull(functorF), checkNonNull(functorG));
  }

  static <F, G> Contravariant<Kind<Kind<EitherK<?, ?, ?>, F>, G>> contravariant(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return EitherKContravariant.instance(checkNonNull(contravariantF), checkNonNull(contravariantG));
  }

  static <F, G> Comonad<Kind<Kind<EitherK<?, ?, ?>, F>, G>> comonad(
      Comonad<F> comonadF, Comonad<G> comonadG) {
    return EitherKComonad.instance(checkNonNull(comonadF), checkNonNull(comonadG));
  }

  @SuppressWarnings("unchecked")
  static <F, G> InjectK<F, Kind<Kind<EitherK<?, ?, ?>, F>, G>> injectEitherKLeft() {
    return EitherKInjectKLeft.INSTANCE;
  }

  static <F, R, G>
      InjectK<F, Kind<Kind<EitherK<?, ?, ?>, G>, R>> injectEitherKRight(InjectK<F, R> inject) {
    return EitherKInjectKRight.instance(checkNonNull(inject));
  }
}

interface EitherKFunctor<F, G> extends Functor<Kind<Kind<EitherK<?, ?, ?>, F>, G>> {

  static <F, G> EitherKFunctor<F, G> instance(Functor<F> functorF, Functor<G> functorG) {
    return new EitherKFunctor<>() {
      @Override
      public Functor<F> f() { return functorF; }
      @Override
      public Functor<G> g() { return functorG; }
    };
  }

  Functor<F> f();
  Functor<G> g();

  @Override
  default <T, R> EitherK<F, G, R> map(
      Kind<Kind<Kind<EitherK<?, ?, ?>, F>, G>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(EitherKOf::narrowK).map(f(), g(), map);
  }
}

interface EitherKContravariant<F, G>
    extends Contravariant<Kind<Kind<EitherK<?, ?, ?>, F>, G>> {

  static <F, G> EitherKContravariant<F, G> instance(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return new EitherKContravariant<>() {
      @Override
      public Contravariant<F> f() { return contravariantF; }
      @Override
      public Contravariant<G> g() { return contravariantG; }
    };
  }

  Contravariant<F> f();
  Contravariant<G> g();

  @Override
  default <A, B> EitherK<F, G, B> contramap(
      Kind<Kind<Kind<EitherK<?, ?, ?>, F>, G>, ? extends A> value, Function1<? super B, ? extends A> map) {
    return value.fix(EitherKOf::<F, G, A>narrowK).contramap(f(), g(), map);
  }
}

interface EitherKComonad<F, G>
    extends Comonad<Kind<Kind<EitherK<?, ?, ?>, F>, G>>, EitherKFunctor<F, G> {

  static <F, G> EitherKComonad<F, G> instance(Comonad<F> comonadF, Comonad<G> comonadG) {
    return new EitherKComonad<>() {
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
  default <A, B> EitherK<F, G, B> coflatMap(
      Kind<Kind<Kind<EitherK<?, ?, ?>, F>, G>, ? extends A> value,
      Function1<? super Kind<Kind<Kind<EitherK<?, ?, ?>, F>, G>, ? extends A>, ? extends B> map) {
    return value.fix(EitherKOf::narrowK).coflatMap(f(), g(), map);
  }

  @Override
  default <A> A extract(Kind<Kind<Kind<EitherK<?, ?, ?>, F>, G>, ? extends A> value) {
    return value.fix(EitherKOf::narrowK).extract(f(), g());
  }
}

interface EitherKInjectKRight<F, G, R>
    extends InjectK<F, Kind<Kind<EitherK<?, ?, ?>, G>, R>> {

  static <F, X, R> EitherKInjectKRight<F, X, R> instance(InjectK<F, R> injectK) {
    return () -> injectK;
  }

  InjectK<F, R> inject();

  @Override
  default  <T> EitherK<G, R, T> inject(Kind<F, ? extends T> value) {
    return EitherK.right(inject().inject(value));
  }
}

interface EitherKInjectKLeft<F, G> extends InjectK<F, Kind<Kind<EitherK<?, ?, ?>, F>, G>> {

  @SuppressWarnings("rawtypes")
  EitherKInjectKLeft INSTANCE = new EitherKInjectKLeft() {};

  @Override
  default  <T> EitherK<F, G, T> inject(Kind<F, ? extends T> value) {
    return EitherK.left(Kind.narrowK(value));
  }
}
