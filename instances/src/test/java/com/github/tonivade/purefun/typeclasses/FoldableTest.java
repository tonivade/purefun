/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Nested.nest;
import static com.github.tonivade.purefun.data.ImmutableList.empty;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.laws.FoldableLaws.verifyLaws;
import static com.github.tonivade.purefun.type.Eval.now;
import static com.github.tonivade.purefun.typeclasses.Foldable.compose;
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
        () -> verifyLaws(IdInstances.foldable(), Id.of("hola").kind1()),
        () -> verifyLaws(TryInstances.foldable(), Try.success("hola").kind1()),
        () -> verifyLaws(EitherInstances.foldable(), Either.right("hola").kind1()),
        () -> verifyLaws(OptionInstances.foldable(), Option.some("hola").kind1()),
        () -> verifyLaws(SequenceInstances.foldable(), listOf("hola").kind1()),
        () -> verifyLaws(compose(SequenceInstances.foldable(), OptionInstances.foldable()), nest(listOf(Option.some("hola").kind1()).kind1())));
  }

  @Test
  public void composed() {
    Foldable<Nested<Sequence.µ, Option.µ>> instance = compose(SequenceInstances.foldable(), OptionInstances.foldable());

    assertEquals(Integer.valueOf(3), instance.fold(Monoid.integer(),
      nest(listOf(Option.some(1).kind1(), Option.<Integer>none().kind1(), Option.some(2).kind1()).kind1())));
  }

  @Test
  public void sequence() {
    Foldable<Sequence.µ> instance = SequenceInstances.foldable();

    assertAll(
        () -> assertEquals("abc", instance.foldLeft(listOf("a", "b", "c").kind1(), "", String::concat)),
        () -> assertEquals("abc", instance.foldRight(listOf("a", "b", "c").kind1(), now(""), (a, lb) -> lb.map(b -> a + b)).value()),
        () -> assertEquals("abc", instance.fold(Monoid.string(), listOf("a", "b", "c").kind1())),
        () -> assertEquals("ABC", instance.foldMap(Monoid.string(), listOf("a", "b", "c").kind1(), String::toUpperCase)),
        () -> assertEquals(Option.some("abc"), instance.reduce(listOf("a", "b", "c").kind1(), String::concat)),
        () -> assertEquals(Id.of("abc"), instance.foldM(IdInstances.monad(), listOf("a", "b", "c").kind1(), "", (a, b) -> Id.of(a + b).kind1())));
  }

  @Test
  public void either() {
    Foldable<Higher1<Either.µ, Throwable>> instance = EitherInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Either.<Throwable, String>left(new Error()).kind1(), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Either.<Throwable, String>right("hola!").kind1(), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.foldRight(Either.<Throwable, String>left(new Error()).kind1(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.foldRight(Either.<Throwable, String>right("hola!").kind1(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Either.<Throwable, String>left(new Error()).kind1())),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Either.<Throwable, String>right("hola!").kind1())),
        () -> assertEquals(Option.none(), instance.reduce(Either.<Throwable, String>left(new Error()).kind1(), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Either.<Throwable, String>right("hola!").kind1(), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Either.<Throwable, String>left(new Error()).kind1(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Either.<Throwable, String>right("hola!").kind1(), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Either.<Throwable, String>left(new Error()).kind1(), empty(), (acc, a) -> Id.of(acc.append(a)).kind1())),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Either.<Throwable, String>right("hola!").kind1(), empty(), (acc, a) -> Id.of(acc.append(a)).kind1())));
  }

  @Test
  public void option() {
    Foldable<Option.µ> instance = OptionInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Option.<String>none().kind1(), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Option.some("hola!").kind1(), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.foldRight(Option.<String>none().kind1(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.foldRight(Option.some("hola!").kind1(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Option.<String>none().kind1())),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Option.some("hola!").kind1())),
        () -> assertEquals(Option.none(), instance.reduce(Option.<String>none().kind1(), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Option.some("hola!").kind1(), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Option.<String>none().kind1(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Option.some("hola!").kind1(), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Option.<String>none().kind1(), empty(), (acc, a) -> Id.of(acc.append(a)).kind1())),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Option.some("hola!").kind1(), empty(), (acc, a) -> Id.of(acc.append(a)).kind1())));
  }

  @Test
  public void try_() {
    Foldable<Try.µ> instance = TryInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Try.<String>failure().kind1(), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Try.success("hola!").kind1(), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.foldRight(Try.<String>failure().kind1(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.foldRight(Try.success("hola!").kind1(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Try.<String>failure().kind1())),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Try.success("hola!").kind1())),
        () -> assertEquals(Option.none(), instance.reduce(Try.<String>failure().kind1(), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Try.success("hola!").kind1(), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Try.<String>failure().kind1(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Try.success("hola!").kind1(), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Try.<String>failure().kind1(), empty(), (acc, a) -> Id.of(acc.append(a)).kind1())),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Try.success("hola!").kind1(), empty(), (acc, a) -> Id.of(acc.append(a)).kind1())));
  }
}
