/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.laws;

import static com.github.tonivade.purefun.type.Eval.now;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Operator2;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Monoid;

public class FoldableLaws {

  public static <F> void verifyLaws(Foldable<F> instance, Kind<F, String> value) {
    assertAll(
        () -> foldLeftConsistentWithFoldMap(instance, Monoid.string(), "", value, String::toUpperCase),
        () -> foldRightConsistentWithFoldMap(instance, Monoid.string(), "", value, String::toUpperCase),
        () -> foldMIdentity(instance, "", value, String::concat),
        () -> reduceConsistentWithFoldM(instance, "", value, String::concat));
  }

  private static <F, A> void reduceConsistentWithFoldM(Foldable<F> instance,
                                                                    A initial,
                                                                    Kind<F, A> value,
                                                                    Operator2<A> combinator) {
    assertEquals(
        instance.foldM(
            OptionInstances.monad(), value, initial,
            combinator.andThen(Option::some).andThen(Option::kind)).fix(OptionOf::narrowK).getOrElseThrow(),
        instance.reduce(value, combinator).getOrElse(initial),
        "reduce consistent law");
  }

  private static <F, A> void foldMIdentity(Foldable<F> instance,
                                                        A initial,
                                                        Kind<F, A> value,
                                                        Operator2<A> combinator) {
    assertEquals(
        instance.foldM(IdInstances.monad(), value, initial, combinator.andThen(Id::of).andThen(Id::kind)),
        Id.of(instance.foldLeft(value, initial, combinator)),
        "foldM identity");
  }

  private static <F, A, B> void foldLeftConsistentWithFoldMap(Foldable<F> instance,
                                                                              Monoid<B> monoid,
                                                                              B initial,
                                                                              Kind<F, ? extends A> value,
                                                                              Function1<? super A, ? extends B> mapper) {
    assertEquals(
        instance.foldLeft(value, initial, (b, a) -> monoid.combine(b, mapper.apply(a))),
        instance.foldMap(monoid, value, mapper),
        "foldLeft consistent with foldMap");
  }

  private static <F, A, B> void foldRightConsistentWithFoldMap(Foldable<F> instance,
                                                                               Monoid<B> monoid,
                                                                               B initial,
                                                                               Kind<F, ? extends A> value,
                                                                               Function1<? super A, ? extends B> mapper) {
    assertEquals(
        instance.<A, B>foldRight(value, now(initial), (a, lb) -> lb.map(b -> monoid.combine(mapper.apply(a), b))).value(),
        instance.foldMap(monoid, value, mapper),
        "foldRight consistent with foldMap");
  }
}
