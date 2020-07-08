/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.instances.TaskInstances;

public class TaskResourceTest<F> extends ResourceTest<Task_> {

  @Override
  protected MonadDefer<Task_> monadDefer() {
    return TaskInstances.monadDefer();
  }
  
  @Override
  protected <T> Resource<Task_, T> makeResource(Kind<Task_, T> acquire, Consumer1<T> release) {
    return TaskInstances.resource(TaskOf.narrowK(acquire), release);
  }

  @Override
  protected <T> T run(Kind<Task_, T> result) {
    return TaskOf.narrowK(result).safeRunSync().get();
  }
}