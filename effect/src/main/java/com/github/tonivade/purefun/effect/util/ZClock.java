/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.URIO;

public interface ZClock {

  <R extends ZClock> ZClock.Service<R> clock();

  static <R extends ZClock> URIO<R, Long> currentTime() {
    return URIO.accessM(env -> env.<R>clock().currentTime());
  }

  static <R extends ZClock> URIO<R, OffsetDateTime> currentDateTime() {
    return URIO.accessM(env -> env.<R>clock().currentDateTime());
  }

  static <R extends ZClock> URIO<R, Unit> sleep(Duration duration) {
    return URIO.accessM(env -> env.<R>clock().sleep(duration));
  }

  interface Service<R extends ZClock> {
    URIO<R, Long> currentTime();
    URIO<R, OffsetDateTime> currentDateTime();
    URIO<R, Unit> sleep(Duration duration);
  }

  static ZClock live() {
    return new ZClock() {
      @Override
      public <R extends ZClock> Service<R> clock() {
        return new Service<R>() {

          @Override
          public URIO<R, Long> currentTime() {
            return URIO.task(System::currentTimeMillis);
          }

          @Override
          public URIO<R, OffsetDateTime> currentDateTime() {
            return URIO.task(OffsetDateTime::now);
          }

          @Override
          public URIO<R, Unit> sleep(Duration duration) {
            return URIO.exec(() -> Thread.sleep(duration.toMillis()));
          }
        };
      }
    };
  }
}
