/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.concurrent;

import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Try;

@ExtendWith(MockitoExtension.class)
public class PromiseTest {

  @Mock
  private Consumer1<Try<String>> consumer;

  private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Test
  public void notCompleted() {
    Promise<String> promise = Promise.make();

    assertFalse(promise.isCompleted());
  }

  @Test
  public void success() {
    String value = "hola mundo!";
    Promise<String> promise = Promise.<String>make().succeeded(value);

    assertTrue(promise.isCompleted());
    assertEquals(Try.success(value), promise.get());
  }

  @Test
  public void failure() {
    IllegalAccessException error = new IllegalAccessException();
    Promise<String> promise = Promise.<String>make().failed(error);

    assertTrue(promise.isCompleted());
    assertEquals(Try.failure(error), promise.get());
  }

  @Test
  public void onCompleteBefore() {
    Try<String> value = Try.success("hola mundo!");
    Promise<String> promise = Promise.make();

    promise.onComplete(consumer);
    promise.tryComplete(value);

    verify(consumer, timeout(1000)).accept(value);
  }

  @Test
  public void onCompleteAfter() {
    Try<String> value = Try.success("hola mundo!");
    Promise<String> promise = Promise.make();

    promise.tryComplete(value);
    promise.onComplete(consumer);

    verify(consumer, timeout(1000)).accept(value);
  }

  @Test
  public void alreadyCompleted() {
    Promise<String> promise = Promise.<String>make().succeeded("hola mundo!");

    assertThrows(IllegalStateException.class, () -> promise.failed(new UnsupportedOperationException()));
  }

  @Test
  public void getTimeout() {
    Promise<String> promise = Promise.make();

    Try<String> result = promise.get(Duration.ofMillis(500));

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof TimeoutException);
  }

  @Test
  public void get() {
    Promise<String> promise = Promise.make();
    Try<String> value = Try.success("hello world!");

    executor.schedule(() -> promise.tryComplete(value), 500, TimeUnit.MILLISECONDS);

    assertEquals(value, promise.get());
  }

  @Test
  public void getInterrupted() throws InterruptedException {
    Promise<String> promise = Promise.make();
    Promise<String> other = Promise.make();

    Future<Unit> job = executor.submit(() -> { other.complete(promise.get()); return unit(); });
    Thread.sleep(100);
    job.cancel(true);

    Try<String> result = other.get();
    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InterruptedException);
  }

  @Test
  public void getInterruptedTimeout() throws InterruptedException {
    Promise<String> promise = Promise.make();
    Promise<String> other = Promise.make();

    Future<Unit> job = executor.submit(() -> {
      other.complete(promise.get(Duration.ofMillis(500)));
      return unit();
    });
    Thread.sleep(100);
    job.cancel(true);

    Try<String> result = other.get();
    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InterruptedException);
  }

  @Test
  public void toFuture() {
    Promise<String> promise = Promise.<String>make().succeeded("hola mundo!");

    Try<String> result = promise.get();

    assertEquals(Try.success("hola mundo!"), result);
  }
}
