/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.typeclasses.Selective;

public class SelectiveLaws {

  public static <F extends Witness> void verifyLaws(Selective<F> selective) {
    Kind<F, Either<String, String>> value = selective.pure(Either.right("Hola Mundo!"));

    assertAll(
        () -> selectiveIdentity(selective, value),
        () -> selectiveDistributivity(selective, value, String::toUpperCase, String::toLowerCase),
        () -> selectiveAssociativity(selective, value, String::toUpperCase, String::concat)
    );
  }

  private static <F extends Witness, A> void selectiveIdentity(Selective<F> selective, Kind<F, Either<A, A>> value) {
    Kind<F, A> select = selective.select(value, selective.pure(identity()));
    Kind<F, A> map = selective.map(value, either -> either.bimap(identity(), identity()).fold(identity(), identity()));
    assertEquals(select, map, "selective identity");
  }

  private static <F extends Witness, A, B> void selectiveDistributivity(Selective<F> selective,
                                                                     Kind<F, Either<A, B>> value,
                                                                     Function1<A, B> f,
                                                                     Function1<A, B> g) {
    Kind<F, B> select = selective.select(value, selective.last(selective.pure(f), selective.pure(g)));

    Kind<F, B> map =
        selective.last(selective.select(value, selective.pure(f)), selective.select(value, selective.pure(g)));

    assertEquals(select, map, "selective distributivity");
  }

  private static <F extends Witness, A, B, C> void selectiveAssociativity(Selective<F> selective,
                                                                          Kind<F, Either<A, B>> value,
                                                                          Function1<A, B> f,
                                                                          Function2<C, A, B> g) {
    Kind<F, Either<C, Function1<? super A, ? extends B>>> y = selective.pure(Either.right(f));
    Kind<F, Function1<? super C, ? extends Function1<? super A, ? extends B>>> z = selective.pure(g.curried());

    Kind<F, Either<A, Either<Tuple2<C, A>, B>>> p =
        selective.map(value, either -> either.map(Either::right));
    Kind<F, Function1<? super A, ? extends Either<Tuple2<C, A>, B>>> q =
        selective.map(y, either -> a -> either.bimap(c -> Tuple.of(c, a), ff -> ff.apply(a)));
    Kind<F, Function1<? super Tuple2<C, A>, ? extends B>> r =
        selective.map(z, ff -> Function2.<C, A, B>uncurried(ff).tupled());
    Kind<F, B> select = selective.select(selective.select(p, q), r);

    Kind<F, Function1<? super A, ? extends B>> select2 = selective.select(y, z);
    assertEquals(selective.select(value, select2), select, "selective associativity");
  }
}
