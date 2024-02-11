/*
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Eq;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Pattern2;
import com.github.tonivade.purefun.free.EitherK;
import com.github.tonivade.purefun.free.EitherKOf;
import com.github.tonivade.purefun.free.EitherK_;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Contravariant;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.InjectK;

public interface EitherKInstances {

  static <F extends Witness, G extends Witness, T> Eq<Kind<Kind<Kind<EitherK_, F>, G>, T>> eq(
      Eq<Kind<F, T>> leftEq, Eq<Kind<G, T>> rightEq) {
    return (a, b) -> Pattern2.<EitherK<F, G, T>, EitherK<F, G, T>, Boolean>build()
        .when((x, y) -> x.isLeft() && y.isLeft())
          .then((x, y) -> leftEq.eqv(x.getLeft(), y.getLeft()))
        .when((x, y) -> x.isRight() && y.isRight())
          .then((x, y) -> rightEq.eqv(x.getRight(), y.getRight()))
        .otherwise()
          .returns(false)
        .apply(EitherKOf.narrowK(a), EitherKOf.narrowK(b));
  }

  static <F extends Witness, G extends Witness> Functor<Kind<Kind<EitherK_, F>, G>> functor(
      Functor<F> functorF, Functor<G> functorG) {
    return EitherKFunctor.instance(checkNonNull(functorF), checkNonNull(functorG));
  }

  static <F extends Witness, G extends Witness> Contravariant<Kind<Kind<EitherK_, F>, G>> contravariant(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return EitherKContravariant.instance(checkNonNull(contravariantF), checkNonNull(contravariantG));
  }

  static <F extends Witness, G extends Witness> Comonad<Kind<Kind<EitherK_, F>, G>> comonad(
      Comonad<F> comonadF, Comonad<G> comonadG) {
    return EitherKComonad.instance(checkNonNull(comonadF), checkNonNull(comonadG));
  }

  @SuppressWarnings("unchecked")
  static <F extends Witness, G extends Witness> InjectK<F, Kind<Kind<EitherK_, F>, G>> injectEitherKLeft() {
    return EitherKInjectKLeft.INSTANCE;
  }

  static <F extends Witness, R extends Witness, G extends Witness>
      InjectK<F, Kind<Kind<EitherK_, G>, R>> injectEitherKRight(InjectK<F, R> inject) {
    return EitherKInjectKRight.instance(checkNonNull(inject));
  }
}

interface EitherKFunctor<F extends Witness, G extends Witness> extends Functor<Kind<Kind<EitherK_, F>, G>> {

  static <F extends Witness, G extends Witness> EitherKFunctor<F, G> instance(Functor<F> functorF, Functor<G> functorG) {
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
      Kind<Kind<Kind<EitherK_, F>, G>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(EitherKOf::narrowK).map(f(), g(), map);
  }
}

interface EitherKContravariant<F extends Witness, G extends Witness>
    extends Contravariant<Kind<Kind<EitherK_, F>, G>> {

  static <F extends Witness, G extends Witness> EitherKContravariant<F, G> instance(
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
      Kind<Kind<Kind<EitherK_, F>, G>, ? extends A> value, Function1<? super B, ? extends A> map) {
    return value.fix(EitherKOf::<F, G, A>narrowK).contramap(f(), g(), map);
  }
}

interface EitherKComonad<F extends Witness, G extends Witness>
    extends Comonad<Kind<Kind<EitherK_, F>, G>>, EitherKFunctor<F, G> {

  static <F extends Witness, G extends Witness> EitherKComonad<F, G> instance(Comonad<F> comonadF, Comonad<G> comonadG) {
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
      Kind<Kind<Kind<EitherK_, F>, G>, ? extends A> value,
      Function1<? super Kind<Kind<Kind<EitherK_, F>, G>, ? extends A>, ? extends B> map) {
    return value.fix(EitherKOf::narrowK).coflatMap(f(), g(), map::apply);
  }

  @Override
  default <A> A extract(Kind<Kind<Kind<EitherK_, F>, G>, ? extends A> value) {
    return value.fix(EitherKOf::narrowK).extract(f(), g());
  }
}

interface EitherKInjectKRight<F extends Witness, G extends Witness, R extends Witness>
    extends InjectK<F, Kind<Kind<EitherK_, G>, R>> {

  static <F extends Witness, X extends Witness, R extends Witness> EitherKInjectKRight<F, X, R> instance(InjectK<F, R> injectK) {
    return () -> injectK;
  }

  InjectK<F, R> inject();

  @Override
  default  <T> EitherK<G, R, T> inject(Kind<F, ? extends T> value) {
    return EitherK.right(inject().inject(value));
  }
}

interface EitherKInjectKLeft<F extends Witness, G extends Witness> extends InjectK<F, Kind<Kind<EitherK_, F>, G>> {

  @SuppressWarnings("rawtypes")
  EitherKInjectKLeft INSTANCE = new EitherKInjectKLeft() {};

  @Override
  default  <T> EitherK<F, G, T> inject(Kind<F, ? extends T> value) {
    return EitherK.left(Kind.narrowK(value));
  }
}
