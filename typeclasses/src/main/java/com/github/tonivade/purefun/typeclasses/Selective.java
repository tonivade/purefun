/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Eval;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.type.Eval.FALSE;
import static com.github.tonivade.purefun.type.Eval.TRUE;

public interface Selective<F extends Witness> extends Applicative<F> {

  <A, B> Kind<F, B> select(Kind<F, Either<A, B>> value, Kind<F, Function1<? super A, ? extends B>> apply);

  default <A, B, C> Kind<F, C> branch(Kind<F, Either<A, B>> value,
                                      Kind<F, Function1<? super A, ? extends C>> applyA,
                                      Kind<F, Function1<? super B, ? extends C>> applyB) {
    Kind<F, Either<A, Either<B, C>>> abc = map(value, either -> either.map(Either::left));
    Kind<F, Function1<? super A, ? extends Either<B, C>>> fabc = map(applyA, fb -> fb.andThen(Either::right));
    return select(select(abc, fabc), applyB);
  }

  default Kind<F, Unit> whenS(Kind<F, Boolean> condition, Kind<F, Unit> apply) {
    return select(SelectiveModule.selector(this, condition), map(apply, Function1::cons));
  }

  default <A> Kind<F, A> ifS(Kind<F, Boolean> condition, Kind<F, A> left, Kind<F, A> right) {
    return branch(SelectiveModule.selector(this, condition), 
        map(left, Function1::cons), map(right, Function1::cons));
  }

  default Kind<F, Boolean> orS(Kind<F, Boolean> condition, Kind<F, Boolean> fa) {
    return ifS(condition, pure(true), fa);
  }

  default Kind<F, Boolean> andS(Kind<F, Boolean> condition, Kind<F, Boolean> fa) {
    return ifS(condition, fa, pure(false));
  }

  default <G extends Witness, A> Eval<Kind<F, Boolean>> anyS(Foldable<G> foldable,
                                                             Kind<G, A> values,
                                                             Function1<? super A, ? extends Kind<F, Boolean>> condition) {
    return foldable.foldRight(values, FALSE.map(this::<Boolean>pure), (a, eb) -> eb.map(b -> orS(b, condition.apply(a))));
  }

  default <G extends Witness, A> Eval<Kind<F, Boolean>> allS(Foldable<G> foldable,
                                                             Kind<G, A> values,
                                                             Function1<? super A, ? extends Kind<F, Boolean>> condition) {
    return foldable.foldRight(values, TRUE.map(this::<Boolean>pure), (a, eb) -> eb.map(b -> andS(b, condition.apply(a))));
  }
}

interface SelectiveModule {

  static <F extends Witness> Kind<F, Either<Unit, Unit>> selector(Selective<F> selective, Kind<F, Boolean> condition) {
    return selective.map(condition, when -> when ? Either.left(unit()) : Either.right(unit()));
  }
}
