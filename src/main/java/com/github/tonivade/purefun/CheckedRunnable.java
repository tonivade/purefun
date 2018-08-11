/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface CheckedRunnable {

  void run() throws Exception;

  default Producer<Try<Nothing>> liftTry() {
    return () -> Try.of(() -> { run(); return nothing(); });
  }

  default Producer<Either<Throwable, Nothing>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default Producer<Option<Nothing>> liftOption() {
    return liftTry().andThen(Try::toOption);
  }
}