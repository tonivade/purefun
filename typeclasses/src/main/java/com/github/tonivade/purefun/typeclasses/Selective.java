/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Eval;

import static com.github.tonivade.purefun.Unit.unit;

public interface Selective<F extends Kind> extends Applicative<F> {

  <A, B> Higher1<F, B> select(Higher1<F, Either<A, B>> value, Higher1<F, Function1<A, B>> apply);

  default <A, B, C> Higher1<F, C> branch(Higher1<F, Either<A, B>> value,
                                         Higher1<F, Function1<A, C>> applyA,
                                         Higher1<F, Function1<B, C>> applyB) {
    Higher1<F, Either<A, Either<B, C>>> abc = map(value, either -> either.map(Either::left));
    Higher1<F, Function1<A, Either<B, C>>> fabc = map(applyA, fb -> fb.andThen(Either::right));
    return select(select(abc, fabc), applyB);
  }

  default Higher1<F, Unit> whenS(Higher1<F, Boolean> condition, Higher1<F, Unit> apply) {
    return select(selector(condition), map(apply, Function1::cons));
  }

  default <A> Higher1<F, A> ifS(Higher1<F, Boolean> condition, Higher1<F, A> left, Higher1<F, A> right) {
    return branch(selector(condition), map(left, Function1::cons), map(right, Function1::cons));
  }

  default Higher1<F, Boolean> orS(Higher1<F, Boolean> condition, Higher1<F, Boolean> fa) {
    return ifS(condition, pure(true), fa);
  }

  default Higher1<F, Boolean> andS(Higher1<F, Boolean> condition, Higher1<F, Boolean> fa) {
    return ifS(condition, fa, pure(false));
  }

  default <G extends Kind, A> Eval<Higher1<F, Boolean>> anyS(Foldable<G> foldable,
                                                             Higher1<G, A> values,
                                                             Function1<A, Higher1<F, Boolean>> condition) {
    return foldable.foldRight(values, Eval.now(pure(false)), (a, eb) -> eb.map(b -> orS(b, condition.apply(a))));
  }

  default <G extends Kind, A> Eval<Higher1<F, Boolean>> allS(Foldable<G> foldable,
                                                             Higher1<G, A> values,
                                                             Function1<A, Higher1<F, Boolean>> condition) {
    return foldable.foldRight(values, Eval.now(pure(true)), (a, eb) -> eb.map(b -> andS(b, condition.apply(a))));
  }

  default Higher1<F, Either<Unit, Unit>> selector(Higher1<F, Boolean> condition) {
    return map(condition, when -> when ? Either.left(unit()) : Either.right(unit()));
  }
}
