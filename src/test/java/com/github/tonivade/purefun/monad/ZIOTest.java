/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

public class ZIOTest {

  @Test
  public void mapRight() {
    Either<Throwable, Integer> result =
        parseInt("1").map(x -> x + 1).run(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void mapLeft() {
    Either<Throwable, Integer> result =
        parseInt("lskjdf").map(x -> x + 1).run(nothing());

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  private ZIO<Nothing, Throwable, Integer> parseInt(String string) {
    return env -> IO.of(() -> Try.of(() -> Integer.parseInt(string)).toEither());
  }
}
