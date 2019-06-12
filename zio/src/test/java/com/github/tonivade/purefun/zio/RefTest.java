/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Unit;
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

    ZIO<Nothing, Nothing, Unit> set = ref.set("Something else");
    ZIO<Nothing, Nothing, String> result = set.andThen(ref.get());

    assertEquals(Either.right("Something else"), result.provide(nothing()));
  }

  @Test
  public void lazySet() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Nothing, Nothing, Unit> lazySet = ref.lazySet("Something else");
    ZIO<Nothing, Nothing, String> result = lazySet.andThen(ref.get());

    assertEquals(Either.right("Something else"), result.provide(nothing()));
  }

  @Test
  public void getAndSet() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Nothing, Nothing, String> result = ref.getAndSet("Something else");
    ZIO<Nothing, Nothing, String> afterUpdate = result.andThen(ref.get());

    assertEquals(Either.right("Hello World!"), result.provide(nothing()));
    assertEquals(Either.right("Something else"), afterUpdate.provide(nothing()));
  }

  @Test
  public void getAndUpdate() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Nothing, Nothing, String> result = ref.getAndUpdate(String::toUpperCase);
    ZIO<Nothing, Nothing, String> afterUpdate = result.andThen(ref.get());

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