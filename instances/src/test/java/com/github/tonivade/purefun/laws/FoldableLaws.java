/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Monoid;

public class FoldableLaws {

  public static <F extends Kind> void verifyLaws(Foldable<F> instance, Higher1<F, String> value) {
    assertAll(
        () -> foldLeftConsistentWithFoldMap(instance, Monoid.string(), "", value, String::toUpperCase),
        () -> foldRightConsistentWithFoldMap(instance, Monoid.string(), "", value, String::toUpperCase),
        () -> foldMIdentity(instance, "", value, String::concat),
        () -> reduceConsistentWithFoldM(instance, "", value, String::concat));
  }

  private static <F extends Kind, A> void reduceConsistentWithFoldM(Foldable<F> instance,
                                                                    A initial,
                                                                    Higher1<F, A> value,
                                                                    Operator2<A> combinator) {
    assertEquals(
        instance.foldM(
            OptionInstances.monad(), value, initial,
            combinator.andThen(Option::some).andThen(Option::kind1)).fix1(Option_::narrowK).get(),
        instance.reduce(value, combinator).getOrElse(initial),
        "reduce consistent law");
  }

  private static <F extends Kind, A> void foldMIdentity(Foldable<F> instance,
                                                        A initial,
                                                        Higher1<F, A> value,
                                                        Operator2<A> combinator) {
    assertEquals(
        instance.foldM(IdInstances.monad(), value, initial, combinator.andThen(Id::of).andThen(Id::kind1)),
        Id.of(instance.foldLeft(value, initial, combinator)),
        "foldM identity");
  }

  private static <F extends Kind, A, B> void foldLeftConsistentWithFoldMap(Foldable<F> instance,
                                                                           Monoid<B> monoid,
                                                                           B initial,
                                                                           Higher1<F, A> value,
                                                                           Function1<A, B> mapper) {
    assertEquals(
        instance.foldLeft(value, initial, (b, a) -> monoid.combine(b, mapper.apply(a))),
        instance.foldMap(monoid, value, mapper),
        "foldLeft consistent with foldMap");
  }

  private static <F extends Kind, A, B> void foldRightConsistentWithFoldMap(Foldable<F> instance,
                                                                            Monoid<B> monoid,
                                                                            B initial,
                                                                            Higher1<F, A> value,
                                                                            Function1<A, B> mapper) {
    assertEquals(
        instance.foldRight(value, now(initial), (a, lb) -> lb.map(b -> monoid.combine(mapper.apply(a), b))).value(),
        instance.foldMap(monoid, value, mapper),
        "foldRight consistent with foldMap");
  }
}
