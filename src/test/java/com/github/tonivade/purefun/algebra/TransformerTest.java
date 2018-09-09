/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.algebra;

import static com.github.tonivade.purefun.type.OptionKind.narrowK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionKind;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryKind;

public class TransformerTest {

  @Test
  public void test() {
    Try<String> success = new OptionToTry().apply(Option.some("hello world!"));

    assertEquals(Try.success("hello world!"), success);
  }
}

class OptionToTry implements Transformer<OptionKind.µ, TryKind.µ> {
  @Override
  public <X> Try<X> apply(Higher<OptionKind.µ, X> from) {
    return narrowK(from).map(Try::success).orElse(Try::failure);
  }
}