/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Producer.failure;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;

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

    Promise<String> promise = future.apply().onComplete(tryConsumer);

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(Try.success("Hello World!")),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.success("Hello World!"), promise.get()));
  }

  @Test
  public void onCompleteFailure() {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Future<String> future = Future.failure(error);

    Promise<String> promise = future.apply().onComplete(tryConsumer);

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(any()),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.failure(error), promise.get()));
  }

  @Test
  public void onSuccess() {
    Future<String> future = Future.success("Hello World!");

    Promise<String> promise = future.apply().onSuccess(consumerSuccess);

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.success("Hello World!"), promise.get()));
  }

  @Test
  public void onSuccessTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100), cons("Hello World!"));

    Promise<String> promise = future.apply().onSuccess(consumerSuccess);

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.success("Hello World!"), promise.get()));
  }

  @Test
  public void onFailure() {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Future<String> future = Future.failure(error);

    Promise<String> promise = future.apply().onFailure(consumerFailure);

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(Try.failure(error), promise.get()));
  }

  @Test
  public void onFailureTimeout() {
    Future<String> future = Future.delay(Duration.ofMillis(100), failure(UnsupportedOperationException::new));

    Promise<String> promise = future.apply().onFailure(consumerFailure);

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(promise::isCompleted),
        () -> assertEquals(UnsupportedOperationException.class, promise.get().getCause().getClass()));
  }

  @Test
  public void map() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.map(String::toUpperCase);

    assertEquals(Try.success("HELLO WORLD!"), result.apply().get());
  }

  @Test
  public void flatMap() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.flatMap(string -> Future.run(string::toUpperCase));

    assertEquals(Try.success("HELLO WORLD!"), result.apply().get());
  }

  @Test
  public void filter() {
    Future<String> future = Future.success("Hello world!");

    Future<String> result = future.filter(string -> string.contains("Hello"));

    assertEquals(Try.success("Hello world!"), result.apply().get());
  }

  @Test
  public void orElse() {
    Future<String> future = Future.failure(new IllegalArgumentException());

    Future<String> result = future.orElse(Future.success("Hello world!"));

    assertEquals(Try.success("Hello world!"), result.apply().get());
  }

  @Test
  public void await() {
    Promise<String> future = Future.success("Hello world!").apply();

    assertEquals(Try.success("Hello world!"), future.get(Duration.ofSeconds(1)));
  }

  @Test
  public void awaitTimeout() {
    Future<String> future = Future.delay(Duration.ofSeconds(1), cons("Hello world!"));

    Try<String> result = future.apply().get(Duration.ofMillis(100));

    assertAll(
        () -> assertTrue(result.isFailure()),
        () -> assertTrue(result.getCause() instanceof TimeoutException));
  }

  @Test
  public void noDeadlock() {
    ExecutorService executor = Executors.newFixedThreadPool(1);
    List<String> result = Collections.synchronizedList(new ArrayList<>());

    currentThread(result).andThen(
        currentThread(result).andThen(
            currentThread(result).andThen(
                currentThread(result)))).apply(executor).get(Duration.ofSeconds(5));

    assertEquals(4, result.size());
  }

  @BeforeEach
  public void setUp() {
    initMocks(this);
  }

  private Future<Unit> currentThread(List<String> result) {
    return Future.exec(() -> result.add(Thread.currentThread().getName()));
  }
}
