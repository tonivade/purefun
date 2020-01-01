/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CheckedRunnableTest {

  @Test
  public void failure() {
    CheckedRunnable task = CheckedRunnable.failure(IllegalAccessException::new);

    assertAll(() -> assertThrows(IllegalAccessException.class, task::run),
              () -> assertTrue(task.asProducer().liftTry().get().isFailure()),
              () -> {
                Catcher catcher = new Catcher();
                task.recover(catcher).run();
                assertNotNull(catcher.get());
              },
              () -> assertThrows(IllegalAccessException.class, task.unchecked()::run));
  }

  @Test
  public void success() {
    CheckedRunnable task = CheckedRunnable.of(() -> System.out.println("hello world"));

    assertAll(() -> assertDoesNotThrow(task::run),
              () -> assertTrue(task.asProducer().liftTry().get().isSuccess()),
              () -> {
                Catcher catcher = new Catcher();
                task.recover(catcher).run();
                assertNull(catcher.get());
              },
              () -> assertDoesNotThrow(task.unchecked()::run));
  }

  private final class Catcher implements Consumer1<Throwable> {

    private Throwable error;

    @Override
    public void run(Throwable error) {
      this.error = error;
    }

    public Throwable get() {
      return error;
    }
  }
}
