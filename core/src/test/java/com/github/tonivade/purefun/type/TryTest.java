/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

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

import com.github.tonivade.purefun.core.Function1;

public class TryTest {

  private final Function1<String, String> toUpperCase = String::toUpperCase;

  @Test
  public void mapSuccess() {
    Try<String> try1 = Try.success("Hola mundo").map(toUpperCase);

    assertEquals(Try.success("HOLA MUNDO"), try1);
  }

  @Test
  public void mapThrowsException() {
    Try<String> try1 = Try.success("Hola mundo").map(Function1.fail(UnsupportedOperationException::new));

    assertTrue(try1.isFailure());
  }

  @Test
  public void mapFailure() {
    Try<String> try1 = Try.<String>failure("Hola mundo").map(toUpperCase);

    assertTrue(try1.isFailure());
    assertEquals("Hola mundo", try1.getCause().getMessage());
  }

  @Test
  public void mapErrorSuccess() {
    Try<String> try1 = Try.success("Hola mundo").mapError(UnsupportedOperationException::new);

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void mapErrorFailure() {
    Try<String> try1 = Try.<String>failure("Hola mundo").mapError(UnsupportedOperationException::new);

    assertTrue(try1.isFailure());
    assertTrue(try1.getCause() instanceof UnsupportedOperationException);
    assertEquals("Hola mundo", try1.getCause().getCause().getMessage());
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
        .filterOrElse(string -> string.startsWith("Hola"), () -> Try.failure("filtered"));

    assertEquals(Try.success("Hola mundo"), try1);
  }

  @Test
  public void filterOrElseNotFilter() {
    Try<String> try1 = Try.success("Hola mundo")
        .filterOrElse(string -> string.startsWith("hola"), () -> Try.failure("filtered"));

    assertTrue(try1.isFailure());
    assertEquals("filtered", try1.getCause().getMessage());
  }

  @Test
  public void filterOrElseFailure() {
    Try<String> try1 = Try.<String>failure("error")
        .filterOrElse(string -> string.startsWith("hola"), () -> Try.failure("or else"));

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

    assertAll(
        () -> assertTrue(success.isSuccess()),
        () -> assertFalse(success.isFailure()),
        () -> assertEquals("Success(Hola mundo)", success.toString()),
        () -> assertEquals("Hola mundo", success.getOrElseThrow()),
        () -> assertEquals(Try.success("Hola mundo"), success),
        () -> assertEquals(Option.some("Hola mundo"), success.toOption()),
        () -> assertEquals(Validation.valid("Hola mundo"), success.toValidation(Throwable::getMessage)),
        () -> assertEquals(Either.right("Hola mundo"), success.toEither()),
        () -> assertEquals(singletonList("Hola mundo"), success.stream().collect(toList())),
        () -> assertThrows(NoSuchElementException.class, success::getCause),
        () -> assertEquals("Hola mundo", success.getOrElseNull()),
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
    Try<String> failure = Try.failure(new UnsupportedOperationException("error"));

    assertAll(
        () -> assertFalse(failure.isSuccess()),
        () -> assertTrue(failure.isFailure()),
        () -> assertEquals("Failure(java.lang.UnsupportedOperationException: error)", failure.toString()),
        () -> assertEquals(Try.failure("error"), Try.failure("error")),
        () -> assertEquals(Option.none(), failure.toOption()),
        () -> assertEquals(Validation.invalid("error"), failure.toValidation(Throwable::getMessage)),
        () -> assertEquals(Either.left(failure.getCause()), failure.toEither()),
        () -> assertEquals("error", failure.getCause().getMessage()),
        () -> assertEquals(emptyList(), failure.stream().collect(toList())),
        () -> assertThrows(UnsupportedOperationException.class, failure::getOrElseThrow),
        () -> assertNull(failure.getOrElseNull()),
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

  private String message() {
    return "Hola mundo";
  }

  private String messageFailure() {
    throw new UnsupportedOperationException("Hola mundo");
  }
}