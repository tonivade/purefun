/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.typeclasses.Bifunctor;

public class BifunctorLaws {

  public static <F extends Witness> void verifyLaws(Bifunctor<F> bifunctor, Kind<Kind<F, String>, String> value) {
    assertAll(() -> identityLaw(bifunctor, value),
              () -> compositionLaw(bifunctor, value,
                  String::toUpperCase, String::toLowerCase, String::toUpperCase, String::toLowerCase),
              () -> mapIdentityLaw(bifunctor, value),
              () -> mapComposition(bifunctor, value, String::toUpperCase, String::toLowerCase),
              () -> leftMapIdentityLaw(bifunctor, value),
              () -> leftMapComposition(bifunctor, value, String::toUpperCase, String::toLowerCase));
  }

  private static <F extends Witness, A, B> void identityLaw(Bifunctor<F> bifunctor, Kind<Kind<F, A>, B> value) {
    assertEquals(value, bifunctor.bimap(value, identity(), identity()), "identity law");
  }

  private static <G extends Witness, A, B, C, D, E, F> void compositionLaw(Bifunctor<G> bifunctor,
                                                                        Kind<Kind<G, A>, B> value,
                                                                        Function1<A, C> f1,
                                                                        Function1<C, D> f2,
                                                                        Function1<B, E> g1,
                                                                        Function1<E, F> g2) {
    assertEquals(bifunctor.bimap(bifunctor.bimap(value, f1, g1), f2, g2),
        bifunctor.bimap(value, f1.andThen(f2), g1.andThen(g2)),
        "composition law");
  }

  private static <F extends Witness, A, B> void mapIdentityLaw(Bifunctor<F> functor, Kind<Kind<F, A>, B> value) {
    assertEquals(value, functor.map(value, identity()), "map identity law");
  }

  private static <F extends Witness, A, B, C, D> void mapComposition(Bifunctor<F> functor,
                                                                  Kind<Kind<F, A>, B> value,
                                                                  Function1<B, C> f,
                                                                  Function1<C, D> g) {
    assertEquals(functor.map(functor.map(value, f), g),
                 functor.map(value, f.andThen(g)),
                 "map composition law");
  }

  private static <F extends Witness, A, B> void leftMapIdentityLaw(Bifunctor<F> functor, Kind<Kind<F, A>, B> value) {
    assertEquals(value, functor.leftMap(value, identity()), "left map identity law");
  }

  private static <F extends Witness, A, B, C, D> void leftMapComposition(Bifunctor<F> functor,
                                                                      Kind<Kind<F, A>, B> value,
                                                                      Function1<A, C> f,
                                                                      Function1<C, D> g) {
    assertEquals(functor.leftMap(functor.leftMap(value, f), g),
                 functor.leftMap(value, f.andThen(g)),
                 "left map composition law");
  }
}
