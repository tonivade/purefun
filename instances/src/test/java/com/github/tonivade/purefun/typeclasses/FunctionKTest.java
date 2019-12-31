/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class FunctionKTest {

  @Test
  public void apply() {
    Higher1<Try.µ, String> success = new OptionToTry().apply(Option.some("hello world!").kind1());

    assertEquals(Try.success("hello world!"), success);
  }

  @Test
  public void andThen() {
    Higher1<Option.µ, String> some = new OptionToTry().andThen(new TryToOption()).apply(Option.some("hello world!").kind1());

    assertEquals(Option.some("hello world!"), some);
  }

  @Test
  public void compose() {
    Higher1<Try.µ, String> some = new OptionToTry().compose(new TryToOption()).apply(Try.success("hello world!").kind1());

    assertEquals(Try.success("hello world!"), some);
  }
}

class OptionToTry implements FunctionK<Option.µ, Try.µ> {
  @Override
  public <X> Higher1<Try.µ, X> apply(Higher1<Option.µ, X> from) {
    return Option.narrowK(from).map(Try::success).getOrElse(Try::failure).kind1();
  }
}

class TryToOption implements FunctionK<Try.µ, Option.µ> {
  @Override
  public <X> Higher1<Option.µ, X> apply(Higher1<Try.µ, X> from) {
    return Try.narrowK(from).toOption().kind1();
  }
}