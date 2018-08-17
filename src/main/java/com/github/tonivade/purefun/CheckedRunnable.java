/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface CheckedRunnable extends Recoverable {

  void run() throws Exception;

  default Runnable recover(Function1<Throwable, Nothing> mapper) {
    return () -> {
      try {
        run();
      } catch(Exception e) {
        mapper.apply(e);
      }
    };
  }

  default Runnable unchecked() {
    return recover(this::sneakyThrow);
  }

  default Producer<Try<Nothing>> liftTry() {
    return () -> Try.of(() -> { run(); return nothing(); });
  }

  default Producer<Either<Throwable, Nothing>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }
}