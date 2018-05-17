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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

public class TryTest {
  final Handler1<String, String> toUpperCase = string -> string.toUpperCase();

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
  }

  @Test
  public void filterFailure() {
    Try<String> try1 = Try.<String>failure("Hola mundo").filter(string -> string.startsWith("hola"));
    
    assertTrue(try1.isFailure());
  }

  @Test
  public void success() {
    Try<String> try1 = Try.success("Hola mundo");
   
    assertAll(() -> assertTrue(try1.isSuccess()),
              () -> assertFalse(try1.isFailure()),
              () -> assertEquals("Hola mundo", try1.get()),
              () -> assertEquals(Try.success("Hola mundo"), try1),
              () -> assertEquals(singletonList("Hola mundo"), try1.stream().collect(toList())),
              () -> assertThrows(IllegalStateException.class, () -> try1.getCause()),
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
    Try<String> try1 = Try.failure("Hola mundo");
    
    assertAll(() -> assertFalse(try1.isSuccess()),
              () -> assertTrue(try1.isFailure()),
              () -> assertEquals(Try.failure("Hola mundo"), Try.failure("Hola mundo")),
              () -> assertEquals("Hola mundo", try1.getCause().getMessage()),
              () -> assertEquals(emptyList(), try1.stream().collect(toList())),
              () -> assertThrows(IllegalStateException.class, () -> try1.get()),
              () -> {
                AtomicReference<Throwable> ref = new AtomicReference<>();
                try1.onFailure(ref::set);
                assertEquals("Hola mundo", ref.get().getMessage());
              },
              () -> {
                AtomicReference<String> ref = new AtomicReference<>();
                try1.onSuccess(ref::set);
                assertNull(ref.get());
              });
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
    throw new RuntimeException("Hola mundo");
  }
}
