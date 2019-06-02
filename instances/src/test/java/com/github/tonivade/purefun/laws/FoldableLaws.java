/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static com.github.tonivade.purefun.type.Eval.now;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator2;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Monoid;

public class FoldableLaws {

  public static <F extends Kind> void verifyLaws(Foldable<F> instance, Higher1<F, String> value) {
    assertAll(
        () -> foldLeftConsistentWithFoldMap(instance, value, String::toUpperCase),
        () -> foldRightConsistentWithFoldMap(instance, value, String::toUpperCase),
        () -> foldMIdentity(instance, value, String::concat),
        () -> reduceConsistentWithFoldM(instance, value, String::concat));
  }

  private static <F extends Kind> void reduceConsistentWithFoldM(Foldable<F> instance,
      Higher1<F, String> value, Operator2<String> combinator) {
    assertEquals(
        instance.foldM(OptionInstances.monad(), value, "", combinator.andThen(Option::some)),
        instance.reduce(value, combinator));
  }

  private static <F extends Kind> void foldMIdentity(Foldable<F> instance,
      Higher1<F, String> value, Operator2<String> combinator) {
    assertEquals(
        instance.foldM(IdInstances.monad(), value, "", combinator.andThen(Id::of)),
        Id.of(instance.foldLeft(value, "", combinator)),
        "foldM identity");
  }

  private static <F extends Kind> void foldLeftConsistentWithFoldMap(Foldable<F> instance,
      Higher1<F, String> value, Function1<String, String> mapper) {
    assertEquals(
        instance.foldLeft(value, "", (b, a) -> b + mapper.apply(a)),
        instance.foldMap(Monoid.string(), value, mapper),
        "foldLeft consistent with foldMap");
  }

  private static <F extends Kind> void foldRightConsistentWithFoldMap(Foldable<F> instance,
      Higher1<F, String> value, Function1<String, String> mapper) {
    assertEquals(
        instance.foldRight(value, now(""), (a, lb) -> lb.map(b -> mapper.apply(a) + b)).value(),
        instance.foldMap(Monoid.string(), value, mapper),
        "foldRight consistent with foldMap");
  }
}
