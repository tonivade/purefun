/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.FlatMap1Laws;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.MappableLaws;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Traverse;

public class TryTest {

  private final Function1<String, String> toUpperCase = string -> string.toUpperCase();

  @Test
  public void mapSuccess() {
    Try<String> try1 = Try.success("Hola mundo").map(toUpperCase);

    assertEquals(Try.success("HOLA MUNDO"), try1);
  }

  @Test
  public void mapFailure() {
    Try<String> try1 = Try.<String>failure("Hola mundo").map(toUpperCase);

    assertTrue(try1.isFailure());
  }

  @Test
  public void flatMapSuccess() {
    Try<String> try1 = Try.success("Hola mundo").flatMap(toUpperCase.liftTry());

    assertEquals(Try.success("HOLA MUNDO"), try1);
  }

  @Test
  public void flatMapFailure() {
    Try<String> try1 = Try.<String>failure("Hola mundo").flatMap(toUpperCase.liftTry());

    assertTrue(try1.isFailure());
  }

  @Test
  public void orElseSuccess() {
    String value = Try.success("Hola mundo").getOrElse("Adios!");

    assertEquals("Hola mundo", value);
  }

  @Test
  public void orElseFailure() {
    String value = Try.<String>failure("Hola mundo").getOrElse("Adios!");

    assertEquals("Adios!", value);
  }

  @Test
  public void notFilter() {
    Try<String> try1 = Try.success("Hola mundo").filter(string -> string.startsWith("Hola"));

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void filter() {
    Try<String> try1 = Try.success("Hola mundo").filter(string -> string.startsWith("hola"));

    assertTrue(try1.isFailure());
    assertEquals("filtered", try1.getCause().getMessage());
  }

  @Test
  public void filterOrElseFilter() {
    Try<String> try1 = Try.success("Hola mundo")
        .filterOrElse(string -> string.startsWith("Hola"), () -> Try.<String>failure("filtered"));

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void filterOrElseNotFilter() {
    Try<String> try1 = Try.success("Hola mundo")
        .filterOrElse(string -> string.startsWith("hola"), () -> Try.<String>failure("filtered"));

    assertTrue(try1.isFailure());
    assertEquals("filtered", try1.getCause().getMessage());
  }

  @Test
  public void filterOrElseFailure() {
    Try<String> try1 = Try.<String>failure("error")
        .filterOrElse(string -> string.startsWith("hola"), () -> Try.<String>failure("or else"));

    assertTrue(try1.isFailure());
    assertEquals("error", try1.getCause().getMessage());
  }

  @Test
  public void filterFailure() {
    Try<String> try1 = Try.<String>failure("error").filter(string -> string.startsWith("hola"));

    assertTrue(try1.isFailure());
    assertEquals("error", try1.getCause().getMessage());
  }

  @Test
  public void foldSuccess() {
    String value = Try.success("Hola mundo").fold(error -> "error", toUpperCase);

    assertEquals("HOLA MUNDO", value);
  }

  @Test
  public void foldFailure() {
    String value = Try.<String>failure("Hola mundo").fold(error -> "error", toUpperCase);

    assertEquals("error", value);
  }

  @Test
  public void success() {
    Try<String> success = Try.success("Hola mundo");

    assertAll(() -> assertTrue(success.isSuccess()),
              () -> assertFalse(success.isFailure()),
              () -> assertEquals("Success(Hola mundo)", success.toString()),
              () -> assertEquals("Hola mundo", success.get()),
              () -> assertEquals(Try.success("Hola mundo"), success),
              () -> assertEquals(Option.some("Hola mundo"), success.toOption()),
              () -> assertEquals(Validation.valid("Hola mundo"), success.toValidation(t -> t.getMessage())),
              () -> assertEquals(Either.right("Hola mundo"), success.toEither()),
              () -> assertEquals(singletonList("Hola mundo"), success.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> success.getCause()),
              () -> {
                AtomicReference<String> ref = new AtomicReference<>();
                success.onSuccess(ref::set);
                assertEquals("Hola mundo", ref.get());
              },
              () -> {
                AtomicReference<Throwable> ref = new AtomicReference<>();
                success.onFailure(ref::set);
                assertNull(ref.get());
              });
  }

  @Test
  public void failure() {
    Try<String> failure = Try.failure("error");

    assertAll(() -> assertFalse(failure.isSuccess()),
              () -> assertTrue(failure.isFailure()),
              () -> assertEquals("Failure(java.lang.Exception: error)", failure.toString()),
              () -> assertEquals(Try.failure("error"), Try.failure("error")),
              () -> assertEquals(Option.none(), failure.toOption()),
              () -> assertEquals(Validation.invalid("error"), failure.toValidation(t -> t.getMessage())),
              () -> assertEquals(Either.left(failure.getCause()), failure.toEither()),
              () -> assertEquals("error", failure.getCause().getMessage()),
              () -> assertEquals(emptyList(), failure.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> failure.get()),
              () -> {
                AtomicReference<Throwable> ref = new AtomicReference<>();
                failure.onFailure(ref::set);
                assertEquals("error", ref.get().getMessage());
              },
              () -> {
                AtomicReference<String> ref = new AtomicReference<>();
                failure.onSuccess(ref::set);
                assertNull(ref.get());
              });
  }

  @Test
  public void recoverFailure() {
    Try<String> try1 = Try.<String>failure("error").recover(t -> "Hola mundo");

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void recoverSuccess() {
    Try<String> try1 = Try.success("Hola mundo").recover(t -> "HOLA MUNDO");

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void recoverWithFailure() {
    Try<String> try1 = Try.<String>failure(new IllegalArgumentException())
        .recoverWith(IllegalArgumentException.class, t -> "Hola mundo");

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void recoverWithDifferentException() {
    Try<String> try1 = Try.<String>failure(new Exception())
        .recoverWith(NullPointerException.class, t -> "Hola mundo");

    assertEquals(Exception.class, try1.getCause().getClass());
  }

  @Test
  public void recoverWithSuccess() {
    Try<String> try1 = Try.success("Hola mundo")
        .recoverWith(NullPointerException.class, t -> "HOLA MUNDO");

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void tryOfFailure() {
    Try<String> try1 = Try.of(this::messageFailure);

    assertTrue(try1.isFailure());
  }

  @Test
  public void tryOfSuccess() {
    Try<String> try1 = Try.of(this::message);

    assertTrue(try1.isSuccess());
  }

  @Test
  public void flatten() {
    Try<Try<String>> tryOfTry = Try.success(Try.success("asdf"));

    assertEquals(Try.success("asdf"), tryOfTry.flatten());
  }

  @Test
  public void flattenError() {
    Try<String> try1 = Try.success("asdf");

    assertThrows(UnsupportedOperationException.class, () -> try1.flatten());
  }

  @Test
  public void tryLaws() {
    MappableLaws.verifyLaws(Try.success("Hola mundo"));
    FlatMap1Laws.verifyLaws(Try.success("Hola mundo"), Try::success);
  }

  @Test
  public void monadError() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Try.µ, Throwable> monadError = TryInstances.monadError();

    Higher1<Try.µ, String> pure = monadError.pure("is not ok");
    Higher1<Try.µ, String> raiseError = monadError.raiseError(error);
    Higher1<Try.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Try.µ, String> ensureOk = monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Try.µ, String> ensureError = monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError),
        () -> assertEquals(Try.success("not an error"), handleError),
        () -> assertEquals(Try.failure(error), ensureError),
        () -> assertEquals(Try.success("is not ok"), ensureOk));
  }

  @Test
  public void foldable() {
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

  @Test
  public void traverse() {
    Traverse<Try.µ> instance = TryInstances.traverse();

    Exception error = new Exception("error");

    assertAll(
        () -> assertEquals(Option.some(Try.success("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Try.success(Option.some("hello!")),
                t -> t.map(String::toUpperCase))),
        () -> assertEquals(Option.some(Try.failure(error)),
            instance.traverse(OptionInstances.applicative(), Try.<Option<String>>failure(error),
                t -> t.map(String::toUpperCase))));
  }

  private String message() {
    return "Hola mundo";
  }

  private String messageFailure() {
    throw new UnsupportedOperationException("Hola mundo");
  }
}