/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Schedule;
import com.github.tonivade.purefun.typeclasses.ScheduleOf;

@SuppressWarnings("unchecked")
public interface ScheduleInstances {

  static <F extends Kind<F, ?>, A> Functor<Schedule<F, A, ?>> functor() {
    return ScheduleFunctor.INSTANCE;
  }
}

interface ScheduleFunctor<F extends Kind<F, ?>, A> extends Functor<Schedule<F, A, ?>> {

  @SuppressWarnings("rawtypes")
  ScheduleFunctor INSTANCE = new ScheduleFunctor() {};

  @Override
  default <T, R> Schedule<F, A, R> map(
      Kind<Schedule<F, A, ?>, ? extends T> value,
      Function1<? super T, ? extends R> mapper) {
    return value.fix(ScheduleOf::toSchedule).map(mapper);
  }
}