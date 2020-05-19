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
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;

public class ReferenceTest {

  @Test
  public void get() {
    Reference<IO_, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.get().fix1(IOOf::narrowK);

    assertEquals("Hello World!", result.unsafeRunSync());
  }

  @Test
  public void set() {
    Reference<IO_, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<Unit> set = ref.set("Something else").fix1(IOOf::narrowK);
    IO<String> result = set.andThen(ref.get().fix1(IOOf::narrowK));

    assertEquals("Something else", result.unsafeRunSync());
  }

  @Test
  public void getAndSet() {
    Reference<IO_, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.getAndSet("Something else").fix1(IOOf::narrowK);
    IO<String> afterUpdate = result.andThen(ref.get().fix1(IOOf::narrowK));

    assertEquals("Hello World!", result.unsafeRunSync());
    assertEquals("Something else", afterUpdate.unsafeRunSync());
  }

  @Test
  public void getAndUpdate() {
    Reference<IO_, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.getAndUpdate(String::toUpperCase).fix1(IOOf::narrowK);
    IO<String> afterUpdate = result.andThen(ref.get().fix1(IOOf::narrowK));

    assertEquals("Hello World!", result.unsafeRunSync());
    assertEquals("HELLO WORLD!", afterUpdate.unsafeRunSync());
  }

  @Test
  public void updateAndGet() {
    Reference<IO_, String> ref = Reference.of(IOInstances.monadDefer(), "Hello World!");

    IO<String> result = ref.updateAndGet(String::toUpperCase).fix1(IOOf::narrowK);

    assertEquals("HELLO WORLD!", result.unsafeRunSync());
  }
}
