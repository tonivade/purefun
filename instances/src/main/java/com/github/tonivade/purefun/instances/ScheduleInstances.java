package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Profunctor;
import com.github.tonivade.purefun.typeclasses.Schedule;
import com.github.tonivade.purefun.typeclasses.ScheduleOf;

@SuppressWarnings("unchecked")
public interface ScheduleInstances {

  static <F, A> Functor<Kind<Kind<Schedule<?, ?, ?>, F>, A>> functor() {
    return ScheduleFunctor.INSTANCE;
  }

  static <F> Profunctor<Kind<Schedule<?, ?, ?>, F>> profunctor() {
    return ScheduleProfunctor.INSTANCE;
  }
}

interface ScheduleFunctor<F, A> extends Functor<Kind<Kind<Schedule<?, ?, ?>, F>, A>> {

  @SuppressWarnings("rawtypes")
  ScheduleFunctor INSTANCE = new ScheduleFunctor() {};

  @Override
  default <T, R> Schedule<F, A, R> map(
      Kind<Kind<Kind<Schedule<?, ?, ?>, F>, A>, ? extends T> value,
      Function1<? super T, ? extends R> mapper) {
    return value.fix(ScheduleOf::toSchedule).map(mapper);
  }
}

interface ScheduleProfunctor<F> extends Profunctor<Kind<Schedule<?, ?, ?>, F>> {

  @SuppressWarnings("rawtypes")
  ScheduleProfunctor INSTANCE = new ScheduleProfunctor() {};

  @Override
  default <A, B, C, D> Schedule<F, C, D> dimap(
      Kind<Kind<Kind<Schedule<?, ?, ?>, F>, A>, ? extends B> value,
      Function1<? super C, ? extends A> contramap,
      Function1<? super B, ? extends D> map) {
    return value.fix(ScheduleOf::toSchedule).dimap(contramap, map);
  }
}