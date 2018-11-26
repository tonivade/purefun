/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.CheckedProducer.failure;
import static com.github.tonivade.purefun.CheckedProducer.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Nothing;

public class FutureTest {

  @Test
  public void onSuccess() {
    Consumer1<String> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.success("Hello World!");

    future.onSuccess(consumer1).await();

    assertAll(
        () -> verify(consumer1, timeout(100)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertTrue(future::isSuccess),
        () -> assertFalse(future::isCanceled),
        () -> assertEquals("Hello World!", future.get()));
  }

  @Test
  public void onSuccessTimeout() {
    Consumer1<String> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.delay(Duration.ofMillis(100), unit("Hello World!"));

    future.onSuccess(consumer1).await();

    assertAll(
        () -> verify(consumer1, timeout(100)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertTrue(future::isSuccess),
        () -> assertFalse(future::isCanceled),
        () -> assertEquals("Hello World!", future.get()));
  }

  @Test
  public void onFailure() {
    Consumer1<Throwable> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.failure(new IllegalArgumentException());

    future.onFailure(consumer1).await();

    assertAll(
        () -> verify(consumer1, timeout(100)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertTrue(future::isFailure),
        () -> assertFalse(future::isCanceled),
        () -> assertThrows(NoSuchElementException.class, future::get));
  }

  @Test
  public void onFailureTimeout() {
    Consumer1<Throwable> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.delay(Duration.ofMillis(100), 
                                         failure(IllegalArgumentException::new));

    future.onFailure(consumer1).await();

    assertAll(
        () -> verify(consumer1, timeout(100)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertTrue(future::isFailure),
        () -> assertFalse(future::isCanceled),
        () -> assertThrows(NoSuchElementException.class, future::get));
  }

  @Test
  public void map() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.map(String::toUpperCase);

    assertEquals(Try.success("HELLO WORLD!"), result.await());
  }

  @Test
  public void flatMap() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.flatMap(string -> Future.run(string::toUpperCase));

    assertEquals(Try.success("HELLO WORLD!"), result.await());
  }

  @Test
  public void flatten() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.map(string -> Future.run(string::toUpperCase)).flatten();

    assertAll(
        () -> assertTrue(result::isSuccess),
        () -> assertTrue(result::isCompleted),
        () -> assertEquals(Try.success("HELLO WORLD!"), result.await()));
  }

  @Test
  public void flattenUnsupported() {
    Future<Nothing> result = Future.success("any").flatten();

    assertAll(
        () -> assertTrue(result::isFailure),
        () -> assertTrue(result::isCompleted),
        () -> assertTrue(result.await().getCause() instanceof UnsupportedOperationException));
  }

  @Test
  public void filter() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.filter(string -> string.contains("Hello"));

    assertEquals(Try.success("Hello world!"), result.await());
  }

  @Test
  public void orElse() {
    Future<String> future = Future.failure(new IllegalArgumentException());

    Future<String> result = future.orElse(Future.success("Hello world!"));

    assertEquals(Try.success("Hello world!"), result.await());
  }

  @Test
  public void await() {
    Future<String> future = Future.success("Hello world!");

    assertEquals(Try.success("Hello world!"), future.await(Duration.ofSeconds(1)));
  }

  @Test
  public void awaitTimeout() {
    Future<String> future = Future.delay(Duration.ofSeconds(1), unit("Hello world!"));

    Try<String> result = future.await(Duration.ofMillis(100));

    assertAll(
        () -> assertTrue(result.isFailure()),
        () -> assertTrue(result.getCause() instanceof NoSuchElementException));
  }

  @Test
  public void cancel() {
    Future<String> future = Future.delay(Duration.ofSeconds(1), unit("Hello world!"));
    
    future.cancel();
    
    assertTrue(future.isCanceled());
  }
}
