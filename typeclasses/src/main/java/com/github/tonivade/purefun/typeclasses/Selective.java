/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Either;

import static com.github.tonivade.purefun.Unit.unit;

public interface Selective<F extends Kind> extends Applicative<F> {

  <A, B> Higher1<F, B> select(Higher1<F, Either<A, B>> value, Higher1<F, Function1<A, B>> apply);

  default <A, B, C> Higher1<F, C> branch(Higher1<F, Either<A, B>> value,
                                         Higher1<F, Function1<A, C>> applyA,
                                         Higher1<F, Function1<B, C>> applyB) {
    Higher1<F, Either<A, Either<B, C>>> abc = map(value, either -> either.map(Either::left));
    Higher1<F, Function1<A, Either<B, C>>> fbc = map(applyA, fb -> fb.andThen(Either::right));
    return select(select(abc, fbc), applyB);
  }

  default Higher1<F, Unit> when(Higher1<F, Boolean> value, Higher1<F, Unit> apply) {
    return select(map(value, when -> when ? Either.left(unit()) : Either.right(unit())), map(apply, a -> (b -> a)));
  }
}
