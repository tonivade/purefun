/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
class PromiseTest {

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  @Test
  void notCompleted() {
    Promise<String> promise = Promise.make();

    assertFalse(promise.isCompleted());
  }

  @Test
  void success() {
    String value = "hola mundo!";
    Promise<String> promise = Promise.<String>make().succeeded(value);

    assertTrue(promise.isCompleted());
    assertEquals(Try.success(value), promise.await());
  }

  @Test
  void failure() {
    IllegalAccessException error = new IllegalAccessException();
    Promise<String> promise = Promise.<String>make().failed(error);

    assertTrue(promise.isCompleted());
    assertEquals(Try.failure(error), promise.await());
  }

  @Test
  void onCompleteBefore(@Mock Consumer1<? super Try<? extends String>> consumer) {
    Try<String> value = Try.success("hola mundo!");
    Promise<String> promise = Promise.make();

    promise.onComplete(consumer);
    promise.tryComplete(value);

    verify(consumer, timeout(1000)).accept(value);
  }

  @Test
  void onCompleteAfter(@Mock Consumer1<? super Try<? extends String>> consumer) {
    Try<String> value = Try.success("hola mundo!");
    Promise<String> promise = Promise.make();

    promise.tryComplete(value);
    promise.onComplete(consumer);

    verify(consumer, timeout(1000)).accept(value);
  }

  @Test
  void alreadyCompleted() {
    Promise<String> promise = Promise.<String>make().succeeded("hola mundo!");

    assertThrows(IllegalStateException.class, () -> promise.failed(new UnsupportedOperationException()));
  }

  @Test
  void getTimeout() {
    Promise<String> promise = Promise.make();

    Try<String> result = promise.await(Duration.ofMillis(500));

    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof TimeoutException);
  }

  @Test
  void get() {
    Promise<String> promise = Promise.make();
    Try<String> value = Try.success("hello world!");

    executor.schedule(() -> promise.tryComplete(value), 500, TimeUnit.MILLISECONDS);

    assertEquals(value, promise.await());
  }

  @Test
  void map() {
    Promise<String> promise = Promise.make();
    Promise<String> map = promise.map(String::toUpperCase);
    Try<String> value = Try.success("hello world!");

    executor.schedule(() -> promise.tryComplete(value), 500, TimeUnit.MILLISECONDS);

    assertEquals(value.map(String::toUpperCase), map.await());
  }

  @Test
  void getInterrupted() throws InterruptedException {
    Promise<String> promise = Promise.make();
    Promise<String> other = Promise.make();

    Future<Unit> job = executor.submit(() -> { other.complete(promise.await()); return unit(); });
    Thread.sleep(100);
    job.cancel(true);

    Try<String> result = other.await();
    assertTrue(result.isFailure());
    assertTrue(result.getCause() instanceof InterruptedException);
  }

  @Test
  void getInterruptedTimeout() throws InterruptedException {
    Promise<String> promise = Promise.make();
    Promise<String> other = Promise.make();

    Future<Unit> job = executor.submit(() -> {
      other.complete(promise.await(Duration.ofMillis(500)));
      return unit();
    });
    Thread.sleep(100);
    job.cancel(true);

    Try<String> result = other.await();
    assertTrue(result.isFailure());
    assertEquals(InterruptedException.class, result.getCause().getClass());
  }

  @Test
  void toFuture() {
    Promise<String> promise = Promise.<String>make().succeeded("hola mundo!");

    Try<String> result = promise.await();

    assertEquals(Try.success("hola mundo!"), result);
  }
}
