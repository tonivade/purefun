package com.github.tonivade.purefun.monad;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tonivade.purefun.Higher;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionKind;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryKind;

import org.junit.jupiter.api.Test;

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
    return OptionKind.narrowK(from).map(Try::success).orElse(Try::failure);
  }
}