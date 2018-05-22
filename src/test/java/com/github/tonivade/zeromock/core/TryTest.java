/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

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

public class TryTest {
  private final Handler1<String, String> toUpperCase = string -> string.toUpperCase();

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
    String value = Try.success("Hola mundo").orElse(() -> "Adios!");
    
    assertEquals("Hola mundo", value);
  }

  @Test
  public void orElseFailure() {
    String value = Try.<String>failure("Hola mundo").orElse(() -> "Adios!");
    
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
    assertEquals("or else", try1.getCause().getMessage());
  }

  @Test
  public void filterFailure() {
    Try<String> try1 = Try.<String>failure("Hola mundo").filter(string -> string.startsWith("hola"));
    
    assertTrue(try1.isFailure());
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
    Try<String> try1 = Try.success("Hola mundo");
   
    assertAll(() -> assertTrue(try1.isSuccess()),
              () -> assertFalse(try1.isFailure()),
              () -> assertEquals("Success(Hola mundo)", try1.toString()),
              () -> assertEquals("Hola mundo", try1.get()),
              () -> assertEquals(Try.success("Hola mundo"), try1),
              () -> assertEquals(Option.some("Hola mundo"), try1.toOption()),
              () -> assertEquals(singletonList("Hola mundo"), try1.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> try1.getCause()),
              () -> {
                AtomicReference<String> ref = new AtomicReference<>();
                try1.onSuccess(ref::set);
                assertEquals("Hola mundo", ref.get());
              },
              () -> {
                AtomicReference<Throwable> ref = new AtomicReference<>();
                try1.onFailure(ref::set);
                assertNull(ref.get());
              });
  }

  @Test
  public void failure() {
    Try<String> try1 = Try.failure("error");
    
    assertAll(() -> assertFalse(try1.isSuccess()),
              () -> assertTrue(try1.isFailure()),
              () -> assertEquals("Failure(java.lang.AssertionError: error)", try1.toString()),
              () -> assertEquals(Option.none(), try1.toOption()),
              () -> assertEquals(Try.failure("error"), Try.failure("error")),
              () -> assertEquals("error", try1.getCause().getMessage()),
              () -> assertEquals(emptyList(), try1.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> try1.get()),
              () -> {
                AtomicReference<Throwable> ref = new AtomicReference<>();
                try1.onFailure(ref::set);
                assertEquals("error", ref.get().getMessage());
              },
              () -> {
                AtomicReference<String> ref = new AtomicReference<>();
                try1.onSuccess(ref::set);
                assertNull(ref.get());
              });
  }
  
  @Test
  public void recoverSuccess() {
    Try<String> try1 = Try.<String>failure("error").recover(t -> "Hola mundo");

    assertEquals(Try.success("Hola mundo"), try1);
  }
  
  @Test
  public void recoverFailure() {
    Try<String> try1 = Try.success("Hola mundo").recover(t -> "HOLA MUNDO");

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
    throw new AssertionError("Hola mundo");
  }
}