/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.effect.Task;

public class TaskResourceTest extends ResourceTest<Task<?>> {

  public TaskResourceTest() {
    super(new Instance<Task<?>>() { });
  }
}