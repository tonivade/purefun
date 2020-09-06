package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.RIO_;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.URIO_;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.typeclasses.Schedule;

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
