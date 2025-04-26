/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.ValidationInstances;
import com.github.tonivade.purefun.laws.SelectiveLaws;
import com.github.tonivade.purefun.type.Validation;

public class SelectiveTest {

  @Test
  public void laws() {
    Selective<Validation<Sequence<String>, ?>> selective =
        ValidationInstances.selective(SequenceInstances.semigroup());

    SelectiveLaws.verifyLaws(selective);
  }
}
