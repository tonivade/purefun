/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

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
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;

@ExtendWith(MockitoExtension.class)
public class FutureTest {

  @Test
  public void onCompleteSuccess(@Mock Consumer1<? super Try<? extends String>> tryConsumer) {
    Future<String> future = Future.success("Hello World!");

    future.onComplete(tryConsumer).await();

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(Try.success("Hello World!")),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.success("Hello World!"), future.await()));
  }

  @Test
  public void onCompleteFailure(@Mock Consumer1<? super Try<? extends String>> tryConsumer) {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Future<String> future = Future.failure(error);

    future.onComplete(tryConsumer).await();

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.failure(error), future.await()));
  }

  @Test
  public void onSuccess(@Mock Consumer1<String> consumerSuccess) {
    Future<String> future = Future.success("Hello World!");

    future.onSuccess(consumerSuccess).await();

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.success("Hello World!"), future.await()));
  }

  @Test
  public void onSuccessTimeout(@Mock Consumer1<String> consumerSuccess) {
    Future<String> future = Future.delay(Duration.ofMillis(100), cons("Hello World!"));

    future.onSuccess(consumerSuccess).await();

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.success("Hello World!"), future.await()));
  }

  @Test
  public void onFailure(@Mock Consumer1<Throwable> consumerFailure) {
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
  public void onFailureTimeout(@Mock Consumer1<Throwable> consumerFailure) {
    Future<String> future = Future.delay(Duration.ofMillis(100), failure(UnsupportedOperationException::new));

    future.onFailure(consumerFailure).await();

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

    Future<String> result = future.flatMap(string -> Future.task(string::toUpperCase));

    assertEquals(Try.success("HELLO WORLD!"), result.await());
  }

  @Test
  public void flatMapFailure() {
    RuntimeException error = new RuntimeException();

    Future<String> result = Future.<String>failure(error).flatMap(string -> Future.task(string::toUpperCase));

    assertEquals(Try.failure(error), result.await());
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

    assertAll(
        () -> assertEquals("Hello world!", success.getOrElse("or else")),
        () -> assertEquals("or else", failure.getOrElse("or else")));
  }

  @Test
  public void getOrElseThrow() {
    Future<String> success = Future.success("Hello world!");
    Future<String> failure = Future.failure(new IllegalArgumentException());

    assertAll(
        () -> assertEquals("Hello world!", success.getOrElseThrow(NoSuchElementException::new)),
        () -> assertThrows(NoSuchElementException.class, () -> failure.getOrElseThrow(NoSuchElementException::new)));
  }

  @Test
  public void toCompletableFuture() {
    Future<String> success = Future.success("Hello world!");
    Future<String> failure = Future.failure(new IllegalArgumentException());

    assertAll(
        () -> assertEquals("Hello world!", success.toCompletableFuture().get()),
        () -> assertThrows(ExecutionException.class, () -> failure.toCompletableFuture().get()));
  }

  @Test
  public void await() {
    Future<String> future = Future.success("Hello world!");

    assertEquals(Try.success("Hello world!"), future.await(Duration.ofSeconds(1)));
  }

  @Test
  public void awaitTimeout() {
    Future<Unit> future = Future.sleep(Duration.ofSeconds(10));

    Try<Unit> result = future.await(Duration.ofMillis(100));

    assertAll(
        () -> assertTrue(result.isFailure()),
        () -> assertTrue(result.getCause() instanceof TimeoutException));
  }

  @Test
  public void cancelled() {
    Future<Unit> future = Future.sleep(Duration.ofSeconds(1));

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
    System.out.println("cancel");

    assertTrue(future.isCancelled());
    assertTrue(future.isCompleted());
    assertTrue(future.await().getCause() instanceof CancellationException);
    Thread.sleep(1500);
    verifyNoMoreInteractions(producer);
  }

  @Test
  public void cancel() throws InterruptedException {
    Future<Unit> future = Future.sleep(Duration.ofSeconds(1));

    Thread.sleep(50);
    future.cancel(true);

    assertTrue(future.isCancelled());
    assertTrue(future.isCompleted());
    assertTrue(future.await().getCause() instanceof CancellationException);
  }

  @Test
  public void sleep() {
    long start = System.currentTimeMillis();

    Future.sleep(Duration.ofSeconds(1)).andThen(Future.success("ok")).await();

    long elapsedTime = System.currentTimeMillis() - start;
    assertTrue(1000 - elapsedTime < 100, () -> "it should wait for almost 1 sec, but it was " + elapsedTime);
  }

  @Test
  public void asyncF(@Mock Producer<Unit> effect) {
    Future<String> async = Future.asyncF(callback -> { 
      callback.accept(Try.success("hello")); 
      return Future.later(effect); 
    });

    assertEquals(Try.success("hello"), async.await());
    verify(effect, timeout(500)).get();
  }

  @Test
  public void async() {
    Future<String> async = Future.async(callback -> callback.accept(Try.success("hello")));

    assertEquals(Try.success("hello"), async.await());
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

  @Test
  public void stackSafety() {
    Future<Integer> sum = sum(100000, 0);

    assertEquals(Try.success(705082704), sum.await(), "future is stack safe :)");
  }

  private Future<Unit> currentThread(Executor executor, List<String> result) {
    return Future.exec(executor, () -> result.add(Thread.currentThread().getName()));
  }

  private Future<Integer> sum(Integer n, Integer sum) {
    if (n == 0) {
      return Future.success(sum);
    }
    return Future.defer(() -> sum( n - 1, sum + n));
  }
}
