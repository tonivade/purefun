/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

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
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Try;

public class FutureTest {

  @Mock
  private Consumer1<Try<String>> tryConsumer;
  @Mock
  private Consumer1<String> consumerSuccess;
  @Mock
  private Consumer1<Throwable> consumerFailure;

  @Test
  public void onCompleteSuccess() {
    Future<String> future = Future.success("Hello World!");

    future.onComplete(tryConsumer).await();

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(Try.success("Hello World!")),
        () -> assertTrue(future::isCompleted),
        () -> assertTrue(future::isSuccess),
        () -> assertFalse(future::isFailure),
        () -> assertFalse(future::isCanceled),
        () -> assertEquals("Hello World!", future.get()));
  }

  @Test
  public void onCompleteFailure() {
    Future<String> future = Future.failure(new IllegalArgumentException());

    future.onComplete(tryConsumer).await();

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isSuccess),
        () -> assertTrue(future::isFailure),
        () -> assertFalse(future::isCanceled),
        () -> assertThrows(NoSuchElementException.class, future::get));
  }

  @Test
  public void onSuccess() {
    Future<String> future = Future.success("Hello World!");

    future.onSuccess(consumerSuccess).await();

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertTrue(future::isSuccess),
        () -> assertFalse(future::isFailure),
        () -> assertFalse(future::isCanceled),
        () -> assertEquals("Hello World!", future.get()));
  }

  @Test
  public void onSuccessTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100), unit("Hello World!"));

    future.onSuccess(consumerSuccess).await();

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertTrue(future::isSuccess),
        () -> assertFalse(future::isFailure),
        () -> assertFalse(future::isCanceled),
        () -> assertEquals("Hello World!", future.get()));
  }

  @Test
  public void onFailure() {
    Future<String> future = Future.failure(new IllegalArgumentException());

    future.onFailure(consumerFailure).await();

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isSuccess),
        () -> assertTrue(future::isFailure),
        () -> assertFalse(future::isCanceled),
        () -> assertThrows(NoSuchElementException.class, future::get));
  }

  @Test
  public void onFailureTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100),
                                         failure(IllegalArgumentException::new));

    future.onFailure(consumerFailure).await();

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isSuccess),
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
    Future<Unit> result = Future.success("any").flatten();

    assertAll(
        () -> assertTrue(result::isFailure),
        () -> assertTrue(result::isCompleted),
        () -> assertTrue(result.getCause() instanceof UnsupportedOperationException));
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
        () -> assertTrue(result.getCause() instanceof TimeoutException));
  }

  @Test
  @Disabled("it fails because someone is interrupting the thread before cancel it")
  public void cancel() {
    Future<String> future = Future.delay(Duration.ofSeconds(5), unit("Hello world!"));

    future.cancel();

    assertTrue(future.isCanceled());
  }
  
  @Test
  public void noDeadlock() {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    List<String> result = Collections.synchronizedList(new ArrayList<>());
    
    currentThread(executor, result).andThen(
        currentThread(executor, result).andThen(
            currentThread(executor, result).andThen(
                currentThread(executor, result)))).await();
    
    assertEquals(4, result.size());
  }

  @BeforeEach
  public void setUp() {
    initMocks(this);
  }

  private Future<Unit> currentThread(ExecutorService executor, List<String> result) {
    return Future.exec(executor, () -> result.add(Thread.currentThread().getName()));
  }
}
