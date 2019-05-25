/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.data.ImmutableList.empty;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Eval.now;
import static com.github.tonivade.purefun.type.Option.none;
import static com.github.tonivade.purefun.type.Option.some;
import static com.github.tonivade.purefun.typeclasses.Foldable.compose;
import static com.github.tonivade.purefun.typeclasses.FoldableLaws.verifyLaws;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nested;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class FoldableTest {

  @Test
  public void laws() {
    assertAll(
        () -> verifyLaws(IdInstances.foldable(), Id.of("hola")),
        () -> verifyLaws(TryInstances.foldable(), Try.success("hola")),
        () -> verifyLaws(EitherInstances.foldable(), Either.right("hola")),
        () -> verifyLaws(OptionInstances.foldable(), Option.some("hola")),
        () -> verifyLaws(SequenceInstances.foldable(), Sequence.listOf("hola")),
        () -> verifyLaws(compose(SequenceInstances.foldable(), OptionInstances.foldable()), nest(listOf(Option.some("hola")))));
  }

  @Test
  public void composed() {
    Foldable<Nested<Sequence.µ, Option.µ>> instance = compose(SequenceInstances.foldable(), OptionInstances.foldable());

    assertEquals(Integer.valueOf(3), instance.fold(Monoid.integer(), nest(listOf(some(1), none(), some(2)))));
  }

  @Test
  public void sequence() {
    Foldable<Sequence.µ> instance = SequenceInstances.foldable();

    assertAll(
        () -> assertEquals("abc", instance.foldLeft(listOf("a", "b", "c"), "", String::concat)),
        () -> assertEquals("abc", instance.foldRight(listOf("a", "b", "c"), now(""), (a, lb) -> lb.map(b -> a + b)).value()),
        () -> assertEquals("abc", instance.fold(Monoid.string(), listOf("a", "b", "c"))),
        () -> assertEquals("ABC", instance.foldMap(Monoid.string(), listOf("a", "b", "c"), String::toUpperCase)),
        () -> assertEquals(Option.some("abc"), instance.reduce(listOf("a", "b", "c"), String::concat)),
        () -> assertEquals(Id.of("abc"), instance.foldM(IdInstances.monad(), listOf("a", "b", "c"), "", (a, b) -> Id.of(a + b))));
  }

  @Test
  public void either() {
    Foldable<Higher1<Either.µ, Throwable>> instance = EitherInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Either.left(new Error()), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Either.right("hola!"), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.foldRight(Either.left(new Error()), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.foldRight(Either.right("hola!"), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Either.left(new Error()))),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Either.right("hola!"))),
        () -> assertEquals(Option.none(), instance.reduce(Either.left(new Error()), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Either.right("hola!"), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Either.left(new Error()), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Either.right("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Either.left(new Error()), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Either.right("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }

  @Test
  public void option() {
    Foldable<Option.µ> instance = OptionInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Option.none(), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Option.some("hola!"), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.foldRight(Option.none(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.foldRight(Option.some("hola!"), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Option.none())),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Option.some("hola!"))),
        () -> assertEquals(Option.none(), instance.reduce(Option.none(), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Option.some("hola!"), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Option.none(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Option.some("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Option.none(), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Option.some("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }

  @Test
  public void try_() {
    Foldable<Try.µ> instance = TryInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Try.failure(), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Try.success("hola!"), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.foldRight(Try.failure(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.foldRight(Try.success("hola!"), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Try.failure())),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Try.success("hola!"))),
        () -> assertEquals(Option.none(), instance.reduce(Try.failure(), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Try.success("hola!"), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Try.failure(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Try.success("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Try.failure(), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Try.success("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }
}
