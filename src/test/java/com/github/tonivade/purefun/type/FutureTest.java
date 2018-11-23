package com.github.tonivade.purefun.type;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tonivade.purefun.Consumer1;

public class FutureTest {

  @Test
  public void onSuccess() {
    Consumer1<String> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.success("Hello World!");

    future.onSuccess(consumer1).await();

    verify(consumer1, timeout(100)).accept("Hello World!");
    assertTrue(future.isCompleted());
    assertEquals("Hello World!", future.get());
  }

  @Test
  public void onSuccessTimeout() {
    Consumer1<String> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.run(() -> {
      Thread.sleep(100);
      return "Hello World!";
    });

    future.onSuccess(consumer1).await();

    verify(consumer1, timeout(100)).accept("Hello World!");
    assertTrue(future::isCompleted);
    assertEquals("Hello World!", future.get());
  }

  @Test
  public void onFailure() {
    Consumer1<Throwable> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.failure(new RuntimeException());

    future.onFailure(consumer1).await();

    verify(consumer1, timeout(100)).accept(any());
    assertTrue(future::isCompleted);
    assertThrows(IllegalStateException.class, future::get);
  }

  @Test
  public void onFailureTimeout() {
    Consumer1<Throwable> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.run(() -> {
      Thread.sleep(100);
      throw new IllegalArgumentException();
    });

    future.onFailure(consumer1).await();

    verify(consumer1, timeout(100)).accept(any());
    assertTrue(future.isCompleted());
    assertThrows(IllegalStateException.class, future::get);
  }

  @Test
  public void map() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.map(String::toUpperCase);

    assertTimeout(ofSeconds(5), () -> assertEquals(Try.success("HELLO WORLD!"), result.await()));
  }

  @Test
  public void flatMap() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.flatMap(string -> Future.run(string::toUpperCase));

    assertTimeout(ofSeconds(5), () -> assertEquals(Try.success("HELLO WORLD!"), result.await()));
  }

  @Test
  public void flatten() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.map(string -> Future.run(string::toUpperCase)).flatten();

    assertTimeout(ofSeconds(5), () -> assertEquals(Try.success("HELLO WORLD!"), result.await()));
  }

  @Test
  public void filter() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.filter(string -> string.contains("Hello"));

    assertTimeout(ofSeconds(5), () -> assertEquals(Try.success("Hello world!"), result.await()));
  }
}
