/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple4;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.IdOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.typeclasses.For;
import com.github.tonivade.purefun.typeclasses.Instances;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CofreeTest {

  @Test
  public void testMap() {
    Cofree<Id<?>, Integer> cofree = Cofree.unfold(Instances.functor(), 0, a -> Id.of(a + 1)).map(x -> x * 2);

    Id<Tuple4<Cofree<Id<?>, Integer>, Cofree<Id<?>, Integer>, Cofree<Id<?>, Integer>, Cofree<Id<?>, Integer>>> tuple4Id =
        For.with(Instances.<Id<?>>monad())
          .then(cofree.tailForced())
          .flatMap(Cofree::tailForced)
          .flatMap(Cofree::tailForced)
          .flatMap(Cofree::tailForced)
          .tuple()
          .fix(IdOf::toId);

    assertEquals(Tuple.of(2, 4, 6, 8), tuple4Id.value().map(Cofree::extract, Cofree::extract, Cofree::extract, Cofree::extract));
  }

  @Test
  public void testFold() {
    Cofree<Option<?>, Integer> cofree = Cofree.unfold(Instances.functor(), 0,
        a -> (a > 100) ? Option.<Integer>none() : Option.some(a + 1));

    assertEquals(5151,
        cofree.<Integer>fold(Instances.traverse(),
            (a, fb) -> Eval.later(() -> fb.fix(OptionOf::toOption).fold(() -> a, x -> x + a))).value());
  }

  @Test
  public void testRun(@Mock Operator1<Integer> plus1) {
    when(plus1.apply(anyInt()))
        .thenReturn(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    Cofree<Option<?>, Integer> cofree = Cofree.unfold(Instances.functor(), 0,
        a -> (a < 10) ? Option.some(plus1.apply(a)) : Option.<Integer>none());

    verify(plus1, never()).apply(anyInt()); // nothing executed

    cofree = cofree.run();

    verify(plus1, times(10)).apply(anyInt()); // after run, then it executes all computations

    assertEquals("0,1,2,3,4,5,6,7,8,9,10",
        cofree.reduceToString(Instances.traverse(), (a, b) -> a + "," + b).value());
  }
}