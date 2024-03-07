/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Try;

@ExtendWith(MockitoExtension.class)
public class ParTest {

  @Test
  public void onSuccess(@Mock Consumer1<String> consumerSuccess) {
    Par<String> par = Par.success("Hello World!").onSuccess(consumerSuccess);

    Try<String> result = par.apply(Future.DEFAULT_EXECUTOR).await();

    assertAll(
        () -> verify(consumerSuccess, timeout(1000)).accept("Hello World!"),
        () -> assertEquals(Try.success("Hello World!"), result));
  }

  @Test
  public void onFailure(@Mock Consumer1<Throwable> consumerFailure) {
    var error = new UnsupportedOperationException();
    Par<String> par = Par.<String>failure(error).onFailure(consumerFailure);

    Try<String> result = par.apply(Future.DEFAULT_EXECUTOR).await();

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
    Par<Unit> run = Par.exec(() -> currentThread(consumer));

    Par<Unit> sequence = Par.sequence(listOf(run, run, run));

    sequence.apply(Future.DEFAULT_EXECUTOR).await();

    ArgumentCaptor<String> captor = ArgumentCaptor.captor();
    verify(consumer, times(3)).accept(captor.capture());

    List<String> values = captor.getAllValues();
    System.out.println(values);
    assertTrue(values.stream().distinct().count() > 1);
  }

  @Test
  public void traverse() {
    Par<String> run = Par.task(this::currentThread);

    Par<Sequence<String>> sequence = Par.traverse(listOf(run, run, run, run, run, run, run));

    Sequence<String> result = sequence.apply(Future.DEFAULT_EXECUTOR).get();

    assertTrue(result.asSet().size() > 1);
  }

  @Test
  public void tuple() {
    Par<String> run = Par.task(this::currentThread);

    Par<Tuple2<String, String>> sequence = Par.tuple(run, run);

    Tuple2<String, String> result = sequence.apply(Future.DEFAULT_EXECUTOR).get();

    assertNotEquals(result.get1(), result.get2());
  }

  @Test
  public void async() {
    Par<String> async = Par.async(callback -> callback.accept(Try.success("hello")));

    assertEquals(Try.success("hello"), async.run().await());
  }

  @Test
  public void sleep() {
    long start = System.currentTimeMillis();

    Par.sleep(Duration.ofSeconds(1)).andThen(Par.success("ok")).run().await();

    long elapsedTime = System.currentTimeMillis() - start;
    assertTrue(1000 - elapsedTime < 100, () -> "it should wait for almost 1 sec, but it was " + elapsedTime);
  }

  private String currentThread() throws InterruptedException {
    var id = "thread-" + Thread.currentThread().threadId();
    Thread.sleep(100);
    return id;
  }

  private void currentThread(Consumer1<String> consumer) throws InterruptedException {
    consumer.accept("thread-" + Thread.currentThread().threadId());
    Thread.sleep(100);
  }
}