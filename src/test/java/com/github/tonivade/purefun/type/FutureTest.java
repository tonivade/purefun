package com.github.tonivade.purefun.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Consumer1;

public class FutureTest {

  @Test
  public void onSuccess() throws InterruptedException {
    ConsumerMock<String> consumer1 = new ConsumerMock<>();

    Future<String> future = Future.success("Hello World!");

    future.onSuccess(consumer1).await();

    consumer1.verify("Hello World!", 100);
    assertTrue(future.isCompleted());
    assertEquals("Hello World!", future.get());
  }

  @Test
  public void onSuccessTimeout() throws InterruptedException {
    ConsumerMock<String> consumer1 = new ConsumerMock<>();

    Future<String> future = Future.run(() -> {
      Thread.sleep(100);
      return "Hello World!";
    });

    future.onSuccess(consumer1).await();

    consumer1.verify("Hello World!", 100);
    assertTrue(future::isCompleted);
    assertEquals("Hello World!", future.get());
  }

  @Test
  public void onFailure() throws InterruptedException {
    ConsumerMock<Throwable> consumer1 = new ConsumerMock<>();
    RuntimeException error = new RuntimeException();
    Future<String> future = Future.failure(error);

    future.onFailure(consumer1).await();

    consumer1.verify(error, 100);
    assertTrue(future::isCompleted);
    assertThrows(IllegalStateException.class, future::get);
  }

  @Test
  public void onFailureTimeout() throws InterruptedException {
    ConsumerMock<Throwable> consumer1 = new ConsumerMock<>();
    RuntimeException error = new RuntimeException();
    Future<String> future = Future.run(() -> {
      Thread.sleep(100);
      throw error;
    });

    future.onFailure(consumer1).await();

    consumer1.verify(error, 100);
    assertTrue(future.isCompleted());
    assertThrows(IllegalStateException.class, future::get);
  }

  @Test
  public void map() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.map(String::toUpperCase);

    assertEquals(Try.success("HELLO WORLD!"), result.await());
  }

  @Test
  public void flatMap() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.flatMap(string -> Future.run(string::toUpperCase));

    assertEquals(Try.success("HELLO WORLD!"), result.await());
  }

  @Test
  public void flatten() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.map(string -> Future.run(string::toUpperCase)).flatten();

    assertEquals(Try.success("HELLO WORLD!"), result.await());
  }

  @Test
  public void filter() {
    Future<String> future = Future.run(() -> "Hello world!");

    Future<String> result = future.filter(string -> string.contains("Hello"));

    assertEquals(Try.success("Hello world!"), result.await());
  }
}

class ConsumerMock<T> implements Consumer1<T> {

  private T value;

  @Override
  public void accept(T value) {
    this.value = value;
  }

  public void verify(T expected, int timeout) throws InterruptedException {
    Thread.sleep(timeout);
    assertEquals(expected, value);
  }
}
