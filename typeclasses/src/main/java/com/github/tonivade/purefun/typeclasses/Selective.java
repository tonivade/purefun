/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;

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

  default Higher1<F, Unit> whenS(Higher1<F, Boolean> value, Higher1<F, Unit> apply) {
    return select(selector(value), map(apply, Function1::cons));
  }

  default <A> Higher1<F, A> ifS(Higher1<F, Boolean> value, Higher1<F, A> left, Higher1<F, A> right) {
    return branch(selector(value), map(left, Function1::cons), map(right, Function1::cons));
  }

  default Higher1<F, Boolean> orS(Higher1<F, Boolean> value, Higher1<F, Boolean> fa) {
    return ifS(value, pure(true), fa);
  }

  default Higher1<F, Boolean> andS(Higher1<F, Boolean> value, Higher1<F, Boolean> fa) {
    return ifS(value, fa, pure(false));
  }

  default <A> Higher1<F, Boolean> anyS(Sequence<A> values, Function1<A, Higher1<F, Boolean>> condition) {
    return values.foldRight(pure(false), (a, b) -> orS(b, condition.apply(a)));
  }

  default <A> Higher1<F, Boolean> allS(Sequence<A> values, Function1<A, Higher1<F, Boolean>> condition) {
    return values.foldRight(pure(true), (a, b) -> andS(b, condition.apply(a)));
  }

  // XXX: StackOverflowError
  default Higher1<F, Unit> whileS(Higher1<F, Boolean> value) {
    return whenS(value, whileS(value));
  }

  default Higher1<F, Either<Unit, Unit>> selector(Higher1<F, Boolean> value) {
    return map(value, when -> when ? Either.left(unit()) : Either.right(unit()));
  }
}
