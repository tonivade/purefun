/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.type.Either;

public class RefTest {

  @Test
  public void get() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.get();

    assertEquals(Either.right("Hello World!"), result.provide(nothing()));
  }

  @Test
  public void set() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.set("Something else").andThen(ref::get);

    assertEquals(Either.right("Something else"), result.provide(nothing()));
  }

  @Test
  public void lazySet() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.lazySet("Something else").andThen(ref::get);

    assertEquals(Either.right("Something else"), result.provide(nothing()));
  }

  @Test
  public void getAndSet() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.getAndSet("Something else");
    ZIO<Object, Object, String> afterUpdate = ref.get();

    assertEquals(Either.right("Hello World!"), result.provide(nothing()));
    assertEquals(Either.right("Something else"), afterUpdate.provide(nothing()));
  }

  @Test
  public void getAndUpdate() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.getAndUpdate(String::toUpperCase);
    ZIO<Object, Object, String> afterUpdate = ref.get();

    assertEquals(Either.right("Hello World!"), result.provide(nothing()));
    assertEquals(Either.right("HELLO WORLD!"), afterUpdate.provide(nothing()));
  }

  @Test
  public void updateAndGet() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.updateAndGet(String::toUpperCase);

    assertEquals(Either.right("HELLO WORLD!"), result.provide(nothing()));
  }
}
