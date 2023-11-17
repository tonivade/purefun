package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.typeclasses.ScheduleOf.toSchedule;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Profunctor;
import com.github.tonivade.purefun.typeclasses.Schedule;
import com.github.tonivade.purefun.typeclasses.Schedule_;

@SuppressWarnings("unchecked")
public interface ScheduleInstances {
  
  static <F extends Witness, A> Functor<Kind<Kind<Schedule_, F>, A>> functor() {
    return ScheduleFunctor.INSTANCE;
  }
  
  static <F extends Witness> Profunctor<Kind<Schedule_, F>> profunctor() {
    return ScheduleProfunctor.INSTANCE;
  }
}

interface ScheduleFunctor<F extends Witness, A> extends Functor<Kind<Kind<Schedule_, F>, A>> {
   
  @SuppressWarnings("rawtypes")
  ScheduleFunctor INSTANCE = new ScheduleFunctor() {};
  
  @Override
  default <T, R> Schedule<F, A, R> map(
      Kind<Kind<Kind<Schedule_, F>, A>, ? extends T> value, 
      Function1<? super T, ? extends R> mapper) {
    return value.fix(toSchedule()).map(mapper);
  }
}

interface ScheduleProfunctor<F extends Witness> extends Profunctor<Kind<Schedule_, F>> {

  @SuppressWarnings("rawtypes")
  ScheduleProfunctor INSTANCE = new ScheduleProfunctor() {};
  
  @Override
  default <A, B, C, D> Schedule<F, C, D> dimap(
      Kind<Kind<Kind<Schedule_, F>, A>, ? extends B> value, 
      Function1<? super C, ? extends A> contramap, 
      Function1<? super B, ? extends D> map) {
    return value.fix(toSchedule()).dimap(contramap, map);
  }
}