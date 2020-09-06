package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.typeclasses.ScheduleOf.toSchedule;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.RIO_;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.URIO_;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Profunctor;
import com.github.tonivade.purefun.typeclasses.Schedule;
import com.github.tonivade.purefun.typeclasses.Schedule_;

public interface ScheduleInstances {

  static Schedule.ScheduleOf<IO_> ofIO() {
    return Schedule.of(IOInstances.monad(), IOInstances.timer());
  }

  static <R> Schedule.ScheduleOf<Kind<Kind<ZIO_, R>, Throwable>> ofZIO() {
    return Schedule.of(ZIOInstances.monad(), ZIOInstances.timer());
  }

  static Schedule.ScheduleOf<UIO_> ofUIO() {
    return Schedule.of(UIOInstances.monad(), UIOInstances.timer());
  }

  static Schedule.ScheduleOf<Kind<EIO_, Throwable>> ofEIO() {
    return Schedule.of(EIOInstances.monad(), EIOInstances.timer());
  }

  static Schedule.ScheduleOf<Task_> ofTask() {
    return Schedule.of(TaskInstances.monad(), TaskInstances.timer());
  }

  static <R> Schedule.ScheduleOf<Kind<URIO_, R>> ofURIO() {
    return Schedule.of(URIOInstances.monad(), URIOInstances.timer());
  }

  static <R> Schedule.ScheduleOf<Kind<RIO_, R>> ofRIO() {
    return Schedule.of(RIOInstances.monad(), RIOInstances.timer());
  }
}

interface ScheduleFunctor<F extends Witness, A> extends Functor<Kind<Kind<Schedule_, F>, A>> {
  
  @Override
  default <T, R> Schedule<F, A, R> map(Kind<Kind<Kind<Schedule_, F>, A>, T> value, Function1<T, R> mapper) {
    return value.fix(toSchedule()).map(mapper);
  }
}

interface ScheduleProfunctor<F extends Witness> extends Profunctor<Kind<Schedule_, F>> {
  
  @Override
  default <A, B, C, D> Schedule<F, C, D> dimap(Kind<Kind<Kind<Schedule_, F>, A>, B> value, Function1<C, A> contramap, Function1<B, D> map) {
    return value.fix(toSchedule()).dimap(contramap, map);
  }
}