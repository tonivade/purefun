/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.type.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ParTest {

  @Test
  public void onCompleteSuccess(@Mock Consumer1<Try<String>> tryConsumer) {
    Par<String> par = Par.success("Hello World!");

    Future<String> future = par.apply(Future.DEFAULT_EXECUTOR);
    future.onComplete(tryConsumer).await();

    assertAll(
        () -> verify(tryConsumer, timeout(1000)).accept(Try.success("Hello World!")),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.success("Hello World!"), future.await()));
  }

  @Test
  public void onCompleteFailure(@Mock Consumer1<Try<String>> tryConsumer) {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Par<String> par = Par.failure(error);

    Future<String> future = par.apply(Future.DEFAULT_EXECUTOR);
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
  public void onFailure(@Mock Consumer1<Throwable> consumerFailure) {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Par<String> par = Par.failure(error);

    Future<String> future = par.apply(Future.DEFAULT_EXECUTOR);
    future.onFailure(consumerFailure);

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertTrue(future::isCompleted),
        () -> assertFalse(future::isCancelled),
        () -> assertEquals(Try.failure(error), future.await()));
  }

  @Test
  public void map() {
    Par<String> par = Par.success("Hello world!");

    Par<String> result = par.map(String::toUpperCase);

    assertEquals(Try.success("HELLO WORLD!"), result.apply(Future.DEFAULT_EXECUTOR).await());
  }

  @Test
  public void flatMap() {
    Par<String> par = Par.success("Hello world!");

    Par<String> result = par.flatMap(string -> Par.task(string::toUpperCase));

    assertEquals(Try.success("HELLO WORLD!"), result.apply(Future.DEFAULT_EXECUTOR).await());
  }

  @Test
  public void flatMapFailure() {
    RuntimeException error = new RuntimeException();

    Par<String> result = Par.<String>failure(error).flatMap(string -> Par.task(string::toUpperCase));

    assertEquals(Try.failure(error), result.apply(Future.DEFAULT_EXECUTOR).await());
  }

  @Test
  public void filter() {
    Par<String> par = Par.success("Hello world!");

    Par<String> result = par.filter(string -> string.contains("Hello"));

    assertEquals(Try.success("Hello world!"), result.apply(Future.DEFAULT_EXECUTOR).await());
  }
}