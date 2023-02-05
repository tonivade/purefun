/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.time.Duration;

import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Try;

public interface MonadDefer<F extends Witness> extends MonadThrow<F>, Bracket<F, Throwable>, Defer<F>, Timer<F> {
  
  @Override
  default Kind<F, Long> currentNanos() {
    return later(System::nanoTime);
  }

  default <A> Kind<F, A> later(Producer<? extends A> later) {
    return defer(() -> Try.of(later::get).fold(this::<A>raiseError, this::<A>pure));
  }

  default Kind<F, Unit> exec(CheckedRunnable later) {
    return later(later.asProducer());
  }
  
  default <A> Kind<F, Tuple2<Duration, A>> timed(Kind<F, A> value) {
    return summarized(value, currentNanos(), (t1, t2) -> Duration.ofNanos(t2 - t1));
  }
  
  default <A, B, C> Kind<F, Tuple2<C, A>> summarized(Kind<F, A> value, Kind<F, B> summary, 
      Function2<? super B, ? super B, ? extends C> combinator) {
    return For.with(this)
      .then(summary)
      .then(value)
      .then(summary)
      .apply((b1, a, b2) -> Tuple.of(combinator.apply(b1, b2), a));
  }

  default <A> Reference<F, A> ref(A value) {
    return Reference.of(this, value);
  }

  default Schedule.ScheduleOf<F> scheduleOf() {
    return Schedule.of(this);
  }
  
  default <A extends AutoCloseable> Resource<F, A> resource(Kind<F, A> acquire) {
    return resource(acquire, AutoCloseable::close);
  }
  
  default <A> Resource<F, A> resource(Kind<F, A> acquire, Consumer1<A> release) {
    return Resource.from(this, acquire, release);
  }
}
