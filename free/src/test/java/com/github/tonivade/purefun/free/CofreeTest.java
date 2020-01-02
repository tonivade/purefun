/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple4;
import com.github.tonivade.purefun.instances.EvalInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.For;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CofreeTest {

  @Test
  public void testMap() {
    Cofree<Id.µ, Integer> cofree = Cofree.unfold(IdInstances.functor(), 0, a -> Id.of(a + 1).kind1()).map(x -> x * 2);

    Id<Tuple4<Cofree<Id.µ, Integer>, Cofree<Id.µ, Integer>, Cofree<Id.µ, Integer>, Cofree<Id.µ, Integer>>> tuple4Id =
        For.with(IdInstances.monad())
          .and(cofree.tail())
          .flatMap(Cofree::tail)
          .flatMap(Cofree::tail)
          .flatMap(Cofree::tail)
          .tuple().fix1(Id::narrowK);

    assertEquals(Tuple.of(2, 4, 6, 8), tuple4Id.get().map(Cofree::head, Cofree::head, Cofree::head, Cofree::head));
  }

  @Test
  public void testFold() {
    Eval<Integer> eval =
        Cofree.unfold(OptionInstances.functor(), 0,
            a -> (a > 100) ? Option.<Integer>none().kind1() : Option.some(a + 1).kind1())
                .fold(EvalInstances.applicative(), OptionInstances.traverse(),
                    (a, fb) -> Eval.later(() -> fb.fix1(Option::narrowK).fold(() -> a, x -> x + a)));

    assertEquals(5151, eval.value());
  }
}