/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Eval;

import static com.github.tonivade.purefun.core.Unit.unit;
import static com.github.tonivade.purefun.type.Eval.FALSE;
import static com.github.tonivade.purefun.type.Eval.TRUE;

public interface Selective<F extends Kind<F, ?>> extends Applicative<F> {

  <A, B> Kind<F, B> select(Kind<F, Either<A, B>> value, Kind<F, Function1<? super A, ? extends B>> apply);

  default <A, B, C> Kind<F, C> branch(Kind<F, Either<A, B>> value,
                                      Kind<F, Function1<? super A, ? extends C>> applyA,
                                      Kind<F, Function1<? super B, ? extends C>> applyB) {
    Kind<F, Either<A, Either<B, C>>> abc = map(value, either -> either.map(Either::left));
    Kind<F, Function1<? super A, ? extends Either<B, C>>> fabc = map(applyA, fb -> fb.andThen(Either::right));
    return select(select(abc, fabc), applyB);
  }

  default Kind<F, Unit> whenS(Kind<F, Boolean> condition, Kind<F, Unit> apply) {
    return select(selector(condition), map(apply, Function1::cons));
  }

  default <A> Kind<F, A> ifS(Kind<F, Boolean> condition, Kind<F, ? extends A> left, Kind<F, ? extends A> right) {
    return branch(selector(condition),
        map(left, Function1::cons), map(right, Function1::cons));
  }

  default Kind<F, Boolean> orS(Kind<F, Boolean> condition, Kind<F, Boolean> fa) {
    return ifS(condition, pure(true), fa);
  }

  default Kind<F, Boolean> andS(Kind<F, Boolean> condition, Kind<F, Boolean> fa) {
    return ifS(condition, fa, pure(false));
  }

  @SuppressWarnings("unchecked")
  default <G extends Kind<G, ?>, A> Eval<Kind<F, Boolean>> anyS(Kind<G, ? extends A> values,
      Function1<? super A, ? extends Kind<F, Boolean>> condition, G...reified) {
    return anyS(Instances.foldable(reified), values, condition);
  }

  default <G extends Kind<G, ?>, A> Eval<Kind<F, Boolean>> anyS(Foldable<G> foldable, Kind<G, ? extends A> values,
      Function1<? super A, ? extends Kind<F, Boolean>> condition) {
    return foldable.foldRight(values, FALSE.map(this::<Boolean>pure), (a, eb) -> eb.map(b -> orS(b, condition.apply(a))));
  }

  @SuppressWarnings("unchecked")
  default <G extends Kind<G, ?>, A> Eval<Kind<F, Boolean>> allS(Kind<G, ? extends A> values,
      Function1<? super A, ? extends Kind<F, Boolean>> condition, G...reified) {
    return allS(Instances.foldable(reified), values, condition);
  }

  default <G extends Kind<G, ?>, A> Eval<Kind<F, Boolean>> allS(Foldable<G> foldable, Kind<G, ? extends A> values,
      Function1<? super A, ? extends Kind<F, Boolean>> condition) {
    return foldable.foldRight(values, TRUE.map(this::<Boolean>pure), (a, eb) -> eb.map(b -> andS(b, condition.apply(a))));
  }

  private Kind<F, Either<Unit, Unit>> selector(Kind<F, Boolean> condition) {
    return map(condition, when -> when ? Either.left(unit()) : Either.right(unit()));
  }
}
