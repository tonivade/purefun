/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Unit;

public class RefTest {

  @Test
  public void get() {
    Ref<String> ref = Ref.of("Hello World!");

    UIO<String> result = ref.get();

    assertEquals("Hello World!", result.unsafeRunSync());
  }

  @Test
  public void set() {
    Ref<String> ref = Ref.of("Hello World!");

    UIO<Unit> set = ref.set("Something else");
    UIO<String> result = set.andThen(ref.get());

    assertEquals("Something else", result.unsafeRunSync());
  }

  @Test
  public void lazySet() {
    Ref<String> ref = Ref.of("Hello World!");

    UIO<Unit> lazySet = ref.lazySet("Something else");
    UIO<String> result = lazySet.andThen(ref.get());

    assertEquals("Something else", result.unsafeRunSync());
  }

  @Test
  public void getAndSet() {
    Ref<String> ref = Ref.of("Hello World!");

    UIO<String> result = ref.getAndSet("Something else");
    UIO<String> afterUpdate = result.andThen(ref.get());

    assertEquals("Hello World!", result.unsafeRunSync());
    assertEquals("Something else", afterUpdate.unsafeRunSync());
  }

  @Test
  public void getAndUpdate() {
    Ref<String> ref = Ref.of("Hello World!");

    UIO<String> result = ref.getAndUpdate(String::toUpperCase);
    UIO<String> afterUpdate = result.andThen(ref.get());

    assertEquals("Hello World!", result.unsafeRunSync());
    assertEquals("HELLO WORLD!", afterUpdate.unsafeRunSync());
  }

  @Test
  public void updateAndGet() {
    Ref<String> ref = Ref.of("Hello World!");

    UIO<String> result = ref.updateAndGet(String::toUpperCase);

    assertEquals("HELLO WORLD!", result.unsafeRunSync());
  }
}
