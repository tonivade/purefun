package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.type.Either;

public class EqTest {

  @Test
  public void sequence() {
    Eq<Higher1<Sequence.µ, Integer>> instance = SequenceInstances.eq(Eq.any());

    assertAll(
        () -> assertTrue(instance.eqv(listOf(1, 2, 3), listOf(1, 2, 3))),
        () -> assertFalse(instance.eqv(listOf(1, 2, 3), listOf(3, 2, 1))),
        () -> assertFalse(instance.eqv(listOf(1, 2), listOf(1, 2, 3))));
  }

  @Test
  public void either() {
    Either<Integer, String> left1 = Either.left(10);
    Either<Integer, String> left2 = Either.left(10);
    Either<Integer, String> right1 = Either.right("hola");
    Either<Integer, String> right2 = Either.right("hola");

    Eq<Higher2<Either.µ, Integer, String>> instance = EitherInstances.eq(Eq.any(), Eq.any());

    assertAll(
        () -> assertTrue(instance.eqv(left1, left2)),
        () -> assertTrue(instance.eqv(right1, right2)),
        () -> assertFalse(instance.eqv(left1, right1)),
        () -> assertFalse(instance.eqv(right2, left2)));
  }
}
