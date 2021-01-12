/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.ImmutableList.empty;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.laws.FoldableLaws.verifyLaws;
import static com.github.tonivade.purefun.type.Eval.now;
import static com.github.tonivade.purefun.typeclasses.Foldable.compose;
import static com.github.tonivade.purefun.typeclasses.Nested.nest;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.data.Sequence_;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Either_;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Try_;

public class FoldableTest {

  @Test
  public void laws() {
    assertAll(
        () -> verifyLaws(IdInstances.foldable(), Id.of("hola")),
        () -> verifyLaws(TryInstances.foldable(), Try.success("hola")),
        () -> verifyLaws(TryInstances.foldable(), Try.<String>failure()),
        () -> verifyLaws(EitherInstances.foldable(), Either.right("hola")),
        () -> verifyLaws(EitherInstances.foldable(), Either.<String, String>left("hola")),
        () -> verifyLaws(OptionInstances.foldable(), Option.some("hola")),
        () -> verifyLaws(OptionInstances.foldable(), Option.<String>none()),
        () -> verifyLaws(SequenceInstances.foldable(), listOf("hola")),
        () -> verifyLaws(SequenceInstances.foldable(), ImmutableList.<String>empty()),
        () -> verifyLaws(ConstInstances.foldable(), Const.<String, String>of("hola")),
        () -> verifyLaws(compose(SequenceInstances.foldable(), OptionInstances.foldable()), nest(listOf(Option.some("hola")))));
  }

  @Test
  public void composed() {
    Foldable<Nested<Sequence_, Option_>> instance = compose(SequenceInstances.foldable(), OptionInstances.foldable());

    assertEquals(Integer.valueOf(3), instance.fold(Monoid.integer(),
      nest(listOf(Option.some(1), Option.<Integer>none(), Option.some(2)))));
  }

  @Test
  public void sequence() {
    Foldable<Sequence_> instance = SequenceInstances.foldable();

    assertAll(
        () -> assertEquals("abc", instance.foldLeft(listOf("a", "b", "c"), "", String::concat)),
        () -> assertEquals("abc", instance.<String, String>foldRight(listOf("a", "b", "c"), now(""), (a, lb) -> lb.map(b -> a + b)).value()),
        () -> assertEquals("abc", instance.fold(Monoid.string(), listOf("a", "b", "c"))),
        () -> assertEquals("ABC", instance.foldMap(Monoid.string(), listOf("a", "b", "c"), String::toUpperCase)),
        () -> assertEquals(Option.some("abc"), instance.reduce(listOf("a", "b", "c"), String::concat)),
        () -> assertEquals(Id.of("abc"), instance.foldM(IdInstances.monad(), listOf("a", "b", "c"), "", (a, b) -> Id.of(a + b))));
  }

  @Test
  public void either() {
    Foldable<Kind<Either_, Throwable>> instance = EitherInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Either.<Throwable, String>left(new Error()), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Either.<Throwable, String>right("hola!"), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.<String, ImmutableList<String>>foldRight(Either.<Throwable, String>left(new Error()), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.<String, ImmutableList<String>>foldRight(Either.<Throwable, String>right("hola!"), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Either.<Throwable, String>left(new Error()))),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Either.<Throwable, String>right("hola!"))),
        () -> assertEquals(Option.none(), instance.reduce(Either.<Throwable, String>left(new Error()), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Either.<Throwable, String>right("hola!"), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Either.<Throwable, String>left(new Error()), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Either.<Throwable, String>right("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Either.<Throwable, String>left(new Error()), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Either.<Throwable, String>right("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }

  @Test
  public void option() {
    Foldable<Option_> instance = OptionInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Option.<String>none(), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Option.some("hola!"), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.<String, ImmutableList<String>>foldRight(Option.<String>none(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.<String, ImmutableList<String>>foldRight(Option.some("hola!"), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Option.<String>none())),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Option.some("hola!"))),
        () -> assertEquals(Option.none(), instance.reduce(Option.<String>none(), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Option.some("hola!"), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Option.<String>none(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Option.some("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Option.<String>none(), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Option.some("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }

  @Test
  public void try_() {
    Foldable<Try_> instance = TryInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Try.<String>failure(), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Try.success("hola!"), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.<String, ImmutableList<String>>foldRight(Try.<String>failure(), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.<String, ImmutableList<String>>foldRight(Try.success("hola!"), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Try.<String>failure())),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Try.success("hola!"))),
        () -> assertEquals(Option.none(), instance.reduce(Try.<String>failure(), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Try.success("hola!"), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(SequenceInstances.monoid(), Try.<String>failure(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(SequenceInstances.monoid(), Try.success("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Try.<String>failure(), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Try.success("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }
}
