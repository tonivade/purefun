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

  static <F extends Kind<F, ?>, G extends Kind<G, ?>, T> Eq<Kind<EitherK<F, G, ?>, T>> eq(
      Eq<Kind<F, T>> leftEq, Eq<Kind<G, T>> rightEq) {
    return (a, b) -> {
      var x = EitherKOf.toEitherK(a);
      var y = EitherKOf.toEitherK(b);
      if (x.isLeft() && y.isLeft()) {
        return leftEq.eqv(x.getLeft(), y.getLeft());
      }
      if (x.isRight() && y.isRight()) {
        return rightEq.eqv(x.getRight(), y.getRight());
      }
      return false;
    };
  }

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> Functor<EitherK<F, G, ?>> functor(
      Functor<F> functorF, Functor<G> functorG) {
    return EitherKFunctor.instance(checkNonNull(functorF), checkNonNull(functorG));
  }

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> Contravariant<EitherK<F, G, ?>> contravariant(
      Contravariant<F> contravariantF, Contravariant<G> contravariantG) {
    return EitherKContravariant.instance(checkNonNull(contravariantF), checkNonNull(contravariantG));
  }

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> Comonad<EitherK<F, G, ?>> comonad(
      Comonad<F> comonadF, Comonad<G> comonadG) {
    return EitherKComonad.instance(checkNonNull(comonadF), checkNonNull(comonadG));
  }

  @SuppressWarnings("unchecked")
  static <F extends Kind<F, ?>, G extends Kind<G, ?>> InjectK<F, EitherK<F, G, ?>> injectEitherKLeft() {
    return EitherKInjectKLeft.INSTANCE;
  }

  static <F extends Kind<F, ?>, R extends Kind<R, ?>, G extends Kind<G, ?>>
      InjectK<F, EitherK<G, R, ?>> injectEitherKRight(InjectK<F, R> inject) {
    return EitherKInjectKRight.instance(checkNonNull(inject));
  }
}

interface EitherKFunctor<F extends Kind<F, ?>, G extends Kind<G, ?>> extends Functor<EitherK<F, G, ?>> {

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> EitherKFunctor<F, G> instance(Functor<F> functorF, Functor<G> functorG) {
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
      Kind<EitherK<F, G, ?>, ? extends T> value, Function1<? super T, ? extends R> map) {
    return value.fix(EitherKOf::toEitherK).map(f(), g(), map);
  }
}

interface EitherKContravariant<F extends Kind<F, ?>, G extends Kind<G, ?>>
    extends Contravariant<EitherK<F, G, ?>> {

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> EitherKContravariant<F, G> instance(
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
      Kind<EitherK<F, G, ?>, ? extends A> value, Function1<? super B, ? extends A> map) {
    return value.fix(EitherKOf::<F, G, A>toEitherK).contramap(f(), g(), map);
  }
}

interface EitherKComonad<F extends Kind<F, ?>, G extends Kind<G, ?>>
    extends Comonad<EitherK<F, G, ?>>, EitherKFunctor<F, G> {

  static <F extends Kind<F, ?>, G extends Kind<G, ?>> EitherKComonad<F, G> instance(Comonad<F> comonadF, Comonad<G> comonadG) {
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
      Kind<EitherK<F, G, ?>, ? extends A> value,
      Function1<? super Kind<EitherK<F, G, ?>, ? extends A>, ? extends B> map) {
    return value.fix(EitherKOf::toEitherK).coflatMap(f(), g(), map);
  }

  @Override
  default <A> A extract(Kind<EitherK<F, G, ?>, ? extends A> value) {
    return value.fix(EitherKOf::toEitherK).extract(f(), g());
  }
}

interface EitherKInjectKRight<F extends Kind<F, ?>, G extends Kind<G, ?>, R extends Kind<R, ?>>
    extends InjectK<F, EitherK<G, R, ?>> {

  static <F extends Kind<F, ?>, X extends Kind<X, ?>, R extends Kind<R, ?>> EitherKInjectKRight<F, X, R> instance(InjectK<F, R> injectK) {
    return () -> injectK;
  }

  InjectK<F, R> inject();

  @Override
  default  <T> EitherK<G, R, T> inject(Kind<F, ? extends T> value) {
    return EitherK.right(inject().inject(value));
  }
}

interface EitherKInjectKLeft<F extends Kind<F, ?>, G extends Kind<G, ?>> extends InjectK<F, EitherK<F, G, ?>> {

  @SuppressWarnings("rawtypes")
  EitherKInjectKLeft INSTANCE = new EitherKInjectKLeft() {};

  @Override
  default  <T> EitherK<F, G, T> inject(Kind<F, ? extends T> value) {
    return EitherK.left(Kind.narrowK(value));
  }
}
