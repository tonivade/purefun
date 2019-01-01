/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;

public class BiFunctorLaws {

  private static final Function1<String, String> toUpperCase = String::toUpperCase;
  private static final Function1<String, String> toLowerCase = String::toLowerCase;

  public static <F extends Kind> void verifyLaws(BiFunctor<F> bifunctor, Higher2<F, String, String> value) {
    assertAll(() -> identityLaw(bifunctor, value),
              () -> compositionLaw(bifunctor, value),
              () -> mapIdentityLaw(bifunctor, value),
              () -> mapComposition(bifunctor, value),
              () -> leftMapIdentityLaw(bifunctor, value),
              () -> leftMapComposition(bifunctor, value));
  }

  private static <F extends Kind> void identityLaw(BiFunctor<F> bifunctor, Higher2<F, String, String> value) {
    assertEquals(value, bifunctor.bimap(value, identity(), identity()), "identity law");
  }

  private static <F extends Kind> void compositionLaw(BiFunctor<F> bifunctor, Higher2<F, String, String> value) {
    assertEquals(bifunctor.bimap(bifunctor.bimap(value, toUpperCase, toUpperCase), toLowerCase, toLowerCase),
        bifunctor.bimap(value, toUpperCase.andThen(toLowerCase), toUpperCase.andThen(toLowerCase)),
        "composition law");
  }

  private static <F extends Kind> void mapIdentityLaw(BiFunctor<F> functor, Higher2<F, String, String> value) {
    assertEquals(value, functor.map(value, identity()), "map identity law");
  }

  private static <F extends Kind> void mapComposition(BiFunctor<F> functor, Higher2<F, String, String> value) {
    assertEquals(functor.map(functor.map(value, toUpperCase), toLowerCase),
                 functor.map(value, toUpperCase.andThen(toLowerCase)),
                 "map composition law");
  }

  private static <F extends Kind> void leftMapIdentityLaw(BiFunctor<F> functor, Higher2<F, String, String> value) {
    assertEquals(value, functor.leftMap(value, identity()), "left map identity law");
  }

  private static <F extends Kind> void leftMapComposition(BiFunctor<F> functor, Higher2<F, String, String> value) {
    assertEquals(functor.leftMap(functor.leftMap(value, toUpperCase), toLowerCase),
                 functor.leftMap(value, toUpperCase.andThen(toLowerCase)),
                 "left map composition law");
  }
}
