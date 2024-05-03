/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;

public class FunctionKTest {

  @Test
  public void apply() {
    Kind<Try<?>, String> success = new OptionToTry().apply(Option.some("hello world!"));

    assertEquals(Try.success("hello world!"), success);
  }

  @Test
  public void andThen() {
    Kind<Option<?>, String> some = new OptionToTry().andThen(new TryToOption()).apply(Option.some("hello world!"));

    assertEquals(Option.some("hello world!"), some);
  }

  @Test
  public void compose() {
    Kind<Try<?>, String> some = new OptionToTry().compose(new TryToOption()).apply(Try.success("hello world!"));

    assertEquals(Try.success("hello world!"), some);
  }
}

class OptionToTry implements FunctionK<Option<?>, Try<?>> {
  @Override
  public <X> Kind<Try<?>, X> apply(Kind<Option<?>, ? extends X> from) {
    return OptionOf.<X>narrowK(from).map(Try::success).getOrElse(Try::failure);
  }
}

class TryToOption implements FunctionK<Try<?>, Option<?>> {
  @Override
  public <X> Kind<Option<?>, X> apply(Kind<Try<?>, ? extends X> from) {
    return TryOf.<X>narrowK(from).toOption();
  }
}