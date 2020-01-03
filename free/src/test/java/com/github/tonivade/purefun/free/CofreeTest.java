/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Operator1;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CofreeTest {

  @Test
  public void testMap() {
    Cofree<Id.µ, Integer> cofree = Cofree.unfold(IdInstances.functor(), 0, a -> Id.of(a + 1).kind1()).map(x -> x * 2);

    Id<Tuple4<Cofree<Id.µ, Integer>, Cofree<Id.µ, Integer>, Cofree<Id.µ, Integer>, Cofree<Id.µ, Integer>>> tuple4Id =
        For.with(IdInstances.monad())
          .and(cofree.tailForced())
          .flatMap(Cofree::tailForced)
          .flatMap(Cofree::tailForced)
          .flatMap(Cofree::tailForced)
          .tuple().fix1(Id::narrowK);

    assertEquals(Tuple.of(2, 4, 6, 8), tuple4Id.get().map(Cofree::extract, Cofree::extract, Cofree::extract, Cofree::extract));
  }

  @Test
  public void testFold() {
    Cofree<Option.µ, Integer> cofree = Cofree.unfold(OptionInstances.functor(), 0,
        a -> (a > 100) ? Option.<Integer>none().kind1() : Option.some(a + 1).kind1());

    assertEquals(5151,
        cofree.<Integer>fold(EvalInstances.applicative(), OptionInstances.traverse(),
            (a, fb) -> Eval.later(() -> fb.fix1(Option::narrowK).fold(() -> a, x -> x + a))).value());
  }

  @Test
  public void testRun(@Mock Operator1<Integer> plus1) {
    when(plus1.apply(anyInt()))
        .thenReturn(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    Cofree<Option.µ, Integer> cofree = Cofree.unfold(OptionInstances.functor(), 0,
        a -> (a < 10) ? Option.some(plus1.apply(a)).kind1() : Option.<Integer>none().kind1());

    verify(plus1, never()).apply(anyInt()); // nothing executed

    cofree = cofree.run();

    verify(plus1, times(10)).apply(anyInt()); // after run, then it executes all computations

    assertEquals("0,1,2,3,4,5,6,7,8,9,10",
        cofree.reduceToString(EvalInstances.applicative(), OptionInstances.traverse(), (a, b) -> a + "," + b).value());
  }
}