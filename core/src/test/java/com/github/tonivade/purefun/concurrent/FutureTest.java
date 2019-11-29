/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Producer.failure;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(MockitoExtension.class)
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

    future.onComplete(tryConsumer);

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(Try.success("Hello World!")),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.success("Hello World!"), future.await()));
  }

  @Test
  public void onCompleteFailure() {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Future<String> future = Future.failure(error);

    future.onComplete(tryConsumer);

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.failure(error), future.await()));
  }

  @Test
  public void onSuccess() {
    Future<String> future = Future.success("Hello World!");

    future.onSuccess(consumerSuccess);

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.success("Hello World!"), future.await()));
  }

  @Test
  public void onSuccessTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100), cons("Hello World!"));

    future.onSuccess(consumerSuccess);

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.success("Hello World!"), future.await()));
  }

  @Test
  public void onFailure() {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Future<String> future = Future.failure(error);

    future.onFailure(consumerFailure);

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.failure(error), future.await()));
  }

  @Test
  public void onFailureTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100), failure(UnsupportedOperationException::new));

    future.onFailure(consumerFailure);

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(UnsupportedOperationException.class, future.await().getCause().getClass()));
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

    Future<String> result = future.flatMap(string -> Future.async(string::toUpperCase));

    assertEquals(Try.success("HELLO WORLD!"), result.await());
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
  public void getOrElse() {
    Future<String> success = Future.success("Hello world!");
    Future<String> failure = Future.failure(new IllegalArgumentException());

    assertAll(() -> assertEquals("Hello world!", success.getOrElse("or else")),
              () -> assertEquals("or else", failure.getOrElse("or else")));
  }

  @Test
  public void getOrElseThrow() {
    Future<String> success = Future.success("Hello world!");
    Future<String> failure = Future.failure(new IllegalArgumentException());

    assertAll(() -> assertEquals("Hello world!", success.getOrElseThrow(NoSuchElementException::new)),
              () -> assertThrows(NoSuchElementException.class, () -> failure.getOrElseThrow(NoSuchElementException::new)));
  }

  @Test
  public void toCompletableFuture() {
    Future<String> success = Future.success("Hello world!");
    Future<String> failure = Future.failure(new IllegalArgumentException());

    assertAll(() -> assertEquals("Hello world!", success.toCompletableFuture().get()),
              () -> assertThrows(ExecutionException.class, () -> failure.toCompletableFuture().get()));
  }

  @Test
  public void await() {
    Future<String> future = Future.success("Hello world!");

    assertEquals(Try.success("Hello world!"), future.await(Duration.ofSeconds(1)));
  }

  @Test
  public void awaitTimeout() {
    Future<String> future = Future.delay(Duration.ofSeconds(10), cons("Hello world!"));

    Try<String> result = future.await(Duration.ofMillis(100));

    assertAll(
        () -> assertTrue(result.isFailure()),
        () -> assertTrue(result.getCause() instanceof TimeoutException));
  }

  @Test
  public void cancelled() throws InterruptedException {
    Future<String> future = Future.delay(Duration.ofSeconds(1), cons("Hello world!"));

    future.cancel(false);

    assertTrue(future.isCancelled());
    assertTrue(future.isCompleted());
    assertTrue(future.await().getCause() instanceof CancellationException);
  }

  @Test
  public void interrupt(@Mock Producer<String> producer) throws InterruptedException {
    Future<String> future = Future.delay(Duration.ofSeconds(1), producer);

    Thread.sleep(50);
    future.cancel(true);

    assertTrue(future.isCancelled());
    assertTrue(future.isCompleted());
    assertTrue(future.await().getCause() instanceof CancellationException);
    Thread.sleep(1500);
    verifyZeroInteractions(producer);
  }

  @Test
  public void notCancelled() {
    Future<String> future = Future.success("Hello world!");

    future.cancel(false);

    assertFalse(future.isCancelled());
    assertTrue(future.isCompleted());
    assertEquals(Try.success("Hello world!"), future.await());
  }

  @Test
  public void noDeadlock() {
    ExecutorService executor = Executors.newFixedThreadPool(1);
    List<String> result = Collections.synchronizedList(new ArrayList<>());

    currentThread(executor, result).andThen(
        currentThread(executor, result).andThen(
            currentThread(executor, result).andThen(
                currentThread(executor, result)))).await(Duration.ofSeconds(5));

    assertEquals(4, result.size());
  }

  @BeforeEach
  public void setUp() {
    initMocks(this);
  }

  private Future<Unit> currentThread(Executor executor, List<String> result) {
    return Future.exec(executor, () -> result.add(Thread.currentThread().getName()));
  }
}
