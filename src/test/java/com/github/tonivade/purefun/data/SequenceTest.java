/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.ImmutableList.toImmutableList;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.data.Sequence.zip;
import static com.github.tonivade.purefun.type.Eval.now;
import static com.github.tonivade.purefun.type.Option.some;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Traverse;

public class SequenceTest {

  @Test
  public void zipTest() {
    ImmutableList<Tuple2<Integer, String>> zipped =
        zip(listOf(0, 1, 2), listOf("a", "b", "c")).collect(toImmutableList());

    assertEquals(listOf(Tuple.of(0, "a"), Tuple.of(1, "b"), Tuple.of(2, "c")), zipped);
  }

  @Test
  public void eq() {
    Eq<Higher1<Sequence.µ, Integer>> instance = Sequence.eq(Eq.any());

    assertAll(
        () -> assertTrue(instance.eqv(listOf(1, 2, 3), listOf(1, 2, 3))),
        () -> assertFalse(instance.eqv(listOf(1, 2, 3), listOf(3, 2, 1))),
        () -> assertFalse(instance.eqv(listOf(1, 2), listOf(1, 2, 3))));
  }

  @Test
  public void traverse() {
    Sequence<Option<String>> seq = listOf(some("a"), some("b"), some("c"));

    Traverse<Sequence.µ> instance = Sequence.traverse();

    Higher1<Option.µ, Higher1<Sequence.µ, String>> result =
        instance.traverse(OptionInstances.applicative(), seq, x -> x.map(String::toUpperCase));

    assertEquals(some(listOf("A", "B", "C")), result);
  }

  @Test
  public void foldable() {
    Foldable<Sequence.µ> instance = Sequence.foldable();

    assertAll(
        () -> assertEquals("abc", instance.foldLeft(listOf("a", "b", "c"), "", String::concat)),
        () -> assertEquals("abc", instance.foldRight(listOf("a", "b", "c"), now(""), (a, lb) -> lb.map(b -> a + b)).value()),
        () -> assertEquals("abc", instance.fold(Monoid.string(), listOf("a", "b", "c"))),
        () -> assertEquals("ABC", instance.foldMap(Monoid.string(), listOf("a", "b", "c"), String::toUpperCase)),
        () -> assertEquals(Option.some("abc"), instance.reduce(listOf("a", "b", "c"), String::concat)),
        () -> assertEquals(Id.of("abc"), instance.foldM(IdInstances.monad(), listOf("a", "b", "c"), "", (a, b) -> Id.of(a + b))));
  }
}
