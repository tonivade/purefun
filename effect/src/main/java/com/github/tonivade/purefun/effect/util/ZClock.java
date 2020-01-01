/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect.util;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.ZIO;

import java.time.Duration;
import java.time.OffsetDateTime;

public interface ZClock {

  <R extends ZClock> ZClock.Service<R> clock();

  static <R extends ZClock> ZIO<R, Nothing, Long> currentTime() {
    return ZIO.accessM(env -> env.<R>clock().currentTime());
  }

  static <R extends ZClock> ZIO<R, Nothing, OffsetDateTime> currentDateTime() {
    return ZIO.accessM(env -> env.<R>clock().currentDateTime());
  }

  static <R extends ZClock> ZIO<R, Nothing, Unit> sleep(Duration duration) {
    return ZIO.accessM(env -> env.<R>clock().sleep(duration));
  }

  interface Service<R extends ZClock> {
    ZIO<R, Nothing, Long> currentTime();
    ZIO<R, Nothing, OffsetDateTime> currentDateTime();
    ZIO<R, Nothing, Unit> sleep(Duration duration);
  }

  static ZClock live() {
    return new ZClock() {
      @Override
      public <R extends ZClock> Service<R> clock() {
        return new Service<R>() {

          @Override
          public ZIO<R, Nothing, Long> currentTime() {
            return UIO.task(System::currentTimeMillis).toZIO();
          }

          @Override
          public ZIO<R, Nothing, OffsetDateTime> currentDateTime() {
            return UIO.task(OffsetDateTime::now).toZIO();
          }

          @Override
          public ZIO<R, Nothing, Unit> sleep(Duration duration) {
            return UIO.exec(() -> Thread.sleep(duration.toMillis())).toZIO();
          }
        };
      }
    };
  }
}
