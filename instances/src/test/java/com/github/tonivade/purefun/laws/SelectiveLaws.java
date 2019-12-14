/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Selective;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectiveLaws {

  public static <F extends Kind> void verifyLaws(Selective<F> selective) {
    Higher1<F, Either<String, String>> value = selective.pure(Either.right("Hola Mundo!"));

    assertAll(
        () -> selectiveIdentity(selective, value),
        () -> selectiveDistributivity(selective, value, String::toUpperCase, String::toLowerCase),
        () -> selectiveAssociativity(selective, value, String::toUpperCase, String::concat)
    );
  }

  private static <F extends Kind, A> void selectiveIdentity(Selective<F> selective, Higher1<F, Either<A, A>> value) {
    Higher1<F, A> select = selective.select(value, selective.pure(identity()));
    Higher1<F, A> map = selective.map(value, either -> either.bimap(identity(), identity()).fold(identity(), identity()));
    assertEquals(select, map, "selective identity");
  }

  private static <F extends Kind, A, B> void selectiveDistributivity(Selective<F> selective,
                                                                     Higher1<F, Either<A, B>> value,
                                                                     Function1<A, B> y,
                                                                     Function1<A, B> z) {
    Higher1<F, B> select = selective.select(value, selective.last(selective.pure(y), selective.pure(z)));

    Higher1<F, B> map =
        selective.last(selective.select(value, selective.pure(y)), selective.select(value, selective.pure(z)));

    assertEquals(select, map, "selective distributivity");
  }

  private static <F extends Kind, A, B, C> void selectiveAssociativity(Selective<F> selective,
                                                                       Higher1<F, Either<A, B>> value,
                                                                       Function1<A, B> x1,
                                                                       Function2<C, A, B> x2) {
    Higher1<F, Either<C, Function1<A, B>>> y = selective.pure(Either.right(x1));
    Higher1<F, Function1<C, Function1<A, B>>> z = selective.pure(x2.curried());

    Higher1<F, Either<A, Either<Tuple2<C, A>, B>>> p =
        selective.map(value, either -> either.map(Either::right));
    Higher1<F, Function1<A, Either<Tuple2<C, A>, B>>> q =
        selective.map(y, either -> a -> either.bimap(c -> Tuple.of(c, a), f -> f.apply(a)));
    Higher1<F, Function1<Tuple2<C, A>, B>> r =
        selective.map(z, f -> Function2.uncurried(f).tupled());
    Higher1<F, B> select = selective.select(selective.select(p, q), r);

    assertEquals(selective.select(value, selective.select(y, z)), select, "selective associativity");
  }
}
