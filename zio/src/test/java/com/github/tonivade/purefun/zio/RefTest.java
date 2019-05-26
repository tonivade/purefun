/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
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

    ZIO<Nothing, Void, Nothing> set = ref.set("Something else");
    Producer<ZIO<Nothing, Void, String>> get = ref::get;
    ZIO<Nothing, Void, String> result = set.andThen(get);

    assertEquals(Either.right("Something else"), result.provide(nothing()));
  }

  @Test
  public void lazySet() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Nothing, Void, Nothing> lazySet = ref.lazySet("Something else");
    Producer<ZIO<Nothing, Void, String>> get = ref::get;
    ZIO<Nothing, Void, String> result = lazySet.andThen(get);

    assertEquals(Either.right("Something else"), result.provide(nothing()));
  }

  @Test
  public void getAndSet() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.getAndSet("Something else");
    Producer<ZIO<Object, Object, String>> get = ref::get;
    ZIO<Object, Object, String> afterUpdate = result.andThen(get);

    assertEquals(Either.right("Hello World!"), result.provide(nothing()));
    assertEquals(Either.right("Something else"), afterUpdate.provide(nothing()));
  }

  @Test
  public void getAndUpdate() {
    Ref<String> ref = Ref.of("Hello World!");

    ZIO<Object, Object, String> result = ref.getAndUpdate(String::toUpperCase);
    Producer<ZIO<Object, Object, String>> get = ref::get;
    ZIO<Object, Object, String> afterUpdate = result.andThen(get);

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
