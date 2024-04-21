/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.type.IdOf.toId;
import static com.github.tonivade.purefun.type.OptionOf.toOption;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple4;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Id_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.typeclasses.For;

@ExtendWith(MockitoExtension.class)
public class CofreeTest {

  @Test
  public void testMap() {
    Cofree<Id_, Integer> cofree = Cofree.unfold(IdInstances.functor(), 0, a -> Id.of(a + 1)).map(x -> x * 2);

    Id<Tuple4<Cofree<Id_, Integer>, Cofree<Id_, Integer>, Cofree<Id_, Integer>, Cofree<Id_, Integer>>> tuple4Id =
        For.with(IdInstances.monad())
          .then(cofree.tailForced())
          .flatMap(Cofree::tailForced)
          .flatMap(Cofree::tailForced)
          .flatMap(Cofree::tailForced)
          .tuple().fix(toId());

    assertEquals(Tuple.of(2, 4, 6, 8), tuple4Id.value().map(Cofree::extract, Cofree::extract, Cofree::extract, Cofree::extract));
  }

  @Test
  public void testFold() {
    Cofree<Option_, Integer> cofree = Cofree.unfold(OptionInstances.functor(), 0,
        a -> (a > 100) ? Option.<Integer>none() : Option.some(a + 1));

    assertEquals(5151,
        cofree.<Integer>fold(OptionInstances.traverse(),
            (a, fb) -> Eval.later(() -> fb.fix(toOption()).fold(() -> a, x -> x + a))).value());
  }

  @Test
  public void testRun(@Mock Operator1<Integer> plus1) {
    when(plus1.apply(anyInt()))
        .thenReturn(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    Cofree<Option_, Integer> cofree = Cofree.unfold(OptionInstances.functor(), 0,
        a -> (a < 10) ? Option.some(plus1.apply(a)) : Option.<Integer>none());

    verify(plus1, never()).apply(anyInt()); // nothing executed

    cofree = cofree.run();

    verify(plus1, times(10)).apply(anyInt()); // after run, then it executes all computations

    assertEquals("0,1,2,3,4,5,6,7,8,9,10",
        cofree.reduceToString(OptionInstances.traverse(), (a, b) -> a + "," + b).value());
  }
}