/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.ImmutableList.empty;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Eval.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.FlatMap1Laws;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.MappableLaws;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Semigroupal;
import com.github.tonivade.purefun.typeclasses.Traverse;

public class OptionTest {
  private final Function1<String, String> toUpperCase = string -> string.toUpperCase();

  @Test
  public void mapSome() {
    Option<String> option = Option.some("Hola mundo").map(toUpperCase);

    assertEquals(Option.some("HOLA MUNDO"), option);
  }

  @Test
  public void mapNone() {
    Option<String> option = Option.<String>none().map(toUpperCase);

    assertTrue(option.isEmpty());
  }

  @Test
  public void flatMapSome() {
    Option<String> option = Option.some("Hola mundo").flatMap(toUpperCase.liftOption());

    assertEquals(Option.some("HOLA MUNDO"), option);
  }

  @Test
  public void flatMapNone() {
    Option<String> option = Option.<String>none().flatMap(toUpperCase.liftOption());

    assertTrue(option.isEmpty());
  }

  @Test
  public void orElseSome() {
    String value = Option.some("Hola mundo").getOrElse("Adios!");

    assertEquals("Hola mundo", value);
  }

  @Test
  public void orElseNone() {
    String value = Option.<String>none().getOrElse("Adios!");

    assertEquals("Adios!", value);
  }

  @Test
  public void notFilter() {
    Option<String> option = Option.some("Hola mundo").filter(string -> string.startsWith("Hola"));

    assertEquals(Option.some("Hola mundo"), option);
  }

  @Test
  public void filter() {
    Option<String> option = Option.some("Hola mundo").filter(string -> string.startsWith("hola"));

    assertTrue(option.isEmpty());
  }

  @Test
  public void filterFailure() {
    Option<String> option = Option.<String>none().filter(string -> string.startsWith("hola"));

    assertTrue(option.isEmpty());
  }

  @Test
  public void foldSome() {
    String value = Option.some("hola").fold(() -> "or else", toUpperCase);

    assertEquals("HOLA", value);
  }

  @Test
  public void foldNone() {
    String value = Option.<String>none().fold(() -> "or else", toUpperCase);

    assertEquals("or else", value);
  }

  @Test
  public void toOptional() {
    Option<String> option = Option.some("hola");

    assertEquals(Optional.of("hola"), option.toOptional());
  }

  @Test
  public void some() {
    Option<String> some = Option.some("Hola mundo");

    assertAll(() -> assertTrue(some.isPresent()),
              () -> assertFalse(some.isEmpty()),
              () -> assertEquals("Hola mundo", some.get()),
              () -> assertEquals("Some(Hola mundo)", some.toString()),
              () -> assertEquals(Optional.of("Hola mundo"), some.toOptional()),
              () -> assertEquals(Option.some("Hola mundo"), some),
              () -> assertEquals(singletonList("Hola mundo"), some.stream().collect(toList())),
              () -> {
                AtomicReference<String> ref = new AtomicReference<>();
                some.ifPresent(ref::set);
                assertEquals("Hola mundo", ref.get());
              });
  }

  @Test
  public void failure() {
    Option<String> none = Option.none();

    assertAll(() -> assertFalse(none.isPresent()),
              () -> assertTrue(none.isEmpty()),
              () -> assertEquals("None", none.toString()),
              () -> assertEquals(Option.none(), none),
              () -> assertEquals(Optional.empty(), none.toOptional()),
              () -> assertEquals(emptyList(), none.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> none.get()),
              () -> {
                AtomicReference<String> ref = new AtomicReference<>();
                none.ifPresent(ref::set);
                assertNull(ref.get());
              });
  }

  @Test
  public void optionOfNone() {
    Option<String> option = Option.of(this::messageNull);

    assertTrue(option.isEmpty());
  }

  @Test
  public void optionOfSome() {
    Option<String> option = Option.of(this::message);

    assertTrue(option.isPresent());
  }

  @Test
  public void optionLaws() {
    MappableLaws.verifyLaws(Option.some("Hola mundo"));
    FlatMap1Laws.verifyLaws(Option.some("Hola mundo"), Option::some);
  }

  @Test
  public void flatten() {
    Option<Option<String>> optionOfOption = Option.some(Option.some("asdf"));

    assertEquals(Option.some("asdf"), optionOfOption.flatten());
  }

  @Test
  public void flattenError() {
    Option<String> option = Option.some("asdf");

    assertThrows(UnsupportedOperationException.class, () -> option.flatten());
  }

  @Test
  public void monad() {
    Monad<Option.µ> monad = OptionInstances.monad();

    Option<String> some = Option.some("asdf");
    Option<String> none = Option.none();

    assertAll(() -> assertEquals(some.map(toUpperCase), monad.map(some, toUpperCase)),
              () -> assertEquals(some.map(toUpperCase), monad.flatMap(some, toUpperCase.liftOption())),
              () -> assertEquals(some, monad.pure("asdf")),
              () -> assertEquals(none, monad.map(none, toUpperCase)),
              () -> assertEquals(none, monad.flatMap(none, toUpperCase.liftOption())),
              () -> assertEquals(some.map(toUpperCase), monad.ap(some, Option.some(toUpperCase))));
  }

  @Test
  public void monadError() {
    MonadError<Option.µ, Nothing> monadError = OptionInstances.monadError();

    Higher1<Option.µ, String> pure = monadError.pure("is not ok");
    Higher1<Option.µ, String> raiseError = monadError.raiseError(nothing());
    Higher1<Option.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Option.µ, String> ensureOk = monadError.ensure(pure, () -> nothing(), value -> "is not ok".equals(value));
    Higher1<Option.µ, String> ensureError = monadError.ensure(pure, () -> nothing(), value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Option.none(), raiseError),
        () -> assertEquals(Option.some("not an error"), handleError),
        () -> assertEquals(Option.none(), ensureError),
        () -> assertEquals(Option.some("is not ok"), ensureOk));
  }

  @Test
  public void traverse() {
    Traverse<Option.µ> instance = OptionInstances.traverse();

    assertAll(
        () -> assertEquals(Try.success(Option.some("HELLO!")),
            instance.traverse(TryInstances.applicative(), Option.some(Try.success("hello!")),
                t -> t.map(String::toUpperCase))),
        () -> assertEquals(Try.success(Option.none()),
            instance.traverse(TryInstances.applicative(), Option.<Try<String>>none(),
                t -> t.map(String::toUpperCase))));
  }

  @Test
  public void semigroupal() {
    Semigroupal<Option.µ> instance = OptionInstances.semigroupal();

    assertAll(
        () -> assertEquals(Option.none(), instance.product(Option.none(), Option.none())),
        () -> assertEquals(Option.none(), instance.product(Option.some(1), Option.none())),
        () -> assertEquals(Option.none(), instance.product(Option.none(), Option.some("a"))),
        () -> assertEquals(Option.some(Tuple.of(1, "a")), instance.product(Option.some(1), Option.some("a"))));
  }

  @Test
  public void foldable() {
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
        () -> assertEquals(empty(), instance.foldMap(Sequence.monoid(), Option.none(), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(Sequence.monoid(), Option.some("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Option.none(), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Option.some("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }

  private String message() {
    return "Hola mundo";
  }

  private String messageNull() {
    return null;
  }
}