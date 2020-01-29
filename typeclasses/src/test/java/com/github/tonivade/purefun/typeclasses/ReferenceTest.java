/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO;

public class ReferenceTest {

  @Test
  public void get() {
    Reference<IO.µ, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.get().fix1(IO::narrowK);

    assertEquals("Hello World!", result.unsafeRunSync());
  }

  @Test
  public void set() {
    Reference<IO.µ, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<Unit> set = ref.set("Something else").fix1(IO::narrowK);
    IO<String> result = set.andThen(ref.get().fix1(IO::narrowK));

    assertEquals("Something else", result.unsafeRunSync());
  }

  @Test
  public void getAndSet() {
    Reference<IO.µ, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.getAndSet("Something else").fix1(IO::narrowK);
    IO<String> afterUpdate = result.andThen(ref.get().fix1(IO::narrowK));

    assertEquals("Hello World!", result.unsafeRunSync());
    assertEquals("Something else", afterUpdate.unsafeRunSync());
  }

  @Test
  public void getAndUpdate() {
    Reference<IO.µ, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.getAndUpdate(String::toUpperCase).fix1(IO::narrowK);
    IO<String> afterUpdate = result.andThen(ref.get().fix1(IO::narrowK));

    assertEquals("Hello World!", result.unsafeRunSync());
    assertEquals("HELLO WORLD!", afterUpdate.unsafeRunSync());
  }

  @Test
  public void updateAndGet() {
    Reference<IO.µ, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.updateAndGet(String::toUpperCase).fix1(IO::narrowK);

    assertEquals("HELLO WORLD!", result.unsafeRunSync());
  }
}
