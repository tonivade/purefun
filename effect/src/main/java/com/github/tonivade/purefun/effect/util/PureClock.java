/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import java.time.Duration;
import java.time.OffsetDateTime;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.URIO;

public interface PureClock {

  <R extends PureClock> PureClock.Service<R> clock();

  static <R extends PureClock> URIO<R, Long> currentTime() {
    return URIO.accessM(env -> env.<R>clock().currentTime());
  }

  static <R extends PureClock> URIO<R, OffsetDateTime> currentDateTime() {
    return URIO.accessM(env -> env.<R>clock().currentDateTime());
  }

  static <R extends PureClock> URIO<R, Unit> sleep(Duration duration) {
    return URIO.accessM(env -> env.<R>clock().sleep(duration));
  }

  interface Service<R extends PureClock> {
    URIO<R, Long> currentTime();
    URIO<R, OffsetDateTime> currentDateTime();
    URIO<R, Unit> sleep(Duration duration);
  }

  static PureClock live() {
    return new PureClock() {
      @Override
      public <R extends PureClock> Service<R> clock() {
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
