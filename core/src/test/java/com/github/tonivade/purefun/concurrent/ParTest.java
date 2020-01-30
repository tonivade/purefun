/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Try;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ParTest {

  @Test
  public void onSuccess(@Mock Consumer1<String> consumerSuccess) {
    Par<String> par = Par.success("Hello World!");

    Try<String> result = par.apply(Future.DEFAULT_EXECUTOR).onSuccess(consumerSuccess).await();

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertEquals(Try.success("Hello World!"), result));
  }

  @Test
  public void onFailure(@Mock Consumer1<Throwable> consumerFailure) {
    UnsupportedOperationException error = new UnsupportedOperationException();
    Par<String> par = Par.failure(error);

    Try<String> result = par.apply(Future.DEFAULT_EXECUTOR).onFailure(consumerFailure).await();

    assertAll(
        () -> verify(consumerFailure, timeout(1000)).accept(any()),
        () -> assertEquals(Try.failure(error), result));
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

  @Test
  public void sequence(@Mock Consumer1<String> consumer) {
    Par<Unit> run = Par.run(() -> currentThread(consumer));

    Par<Unit> sequence = Par.sequence(listOf(run, run, run));

    sequence.apply(Future.DEFAULT_EXECUTOR).await();

    verify(consumer).accept("pool-1-thread-1");
    verify(consumer).accept("pool-1-thread-2");
    verify(consumer).accept("pool-1-thread-3");
  }

  @Test
  public void traverse() {
    Par<String> run = Par.task(() -> Thread.currentThread().getName());

    Par<Sequence<String>> sequence = Par.traverse(listOf(run, run, run, run, run, run, run));

    Sequence<String> result = sequence.apply(Future.DEFAULT_EXECUTOR).get();

    assertTrue(result.asSet().size() > 1);
  }

  @Test
  public void tuple() {
    Par<String> run = Par.task(() -> Thread.currentThread().getName());

    Par<Tuple2<String, String>> sequence = Par.tuple(run, run);

    Tuple2<String, String> result = sequence.apply(Future.DEFAULT_EXECUTOR).get();

    assertNotEquals(result.get1(), result.get2());
  }

  private void currentThread(Consumer1<String> consumer) throws InterruptedException {
    consumer.accept(Thread.currentThread().getName());
    Thread.sleep(1000);
  }
}