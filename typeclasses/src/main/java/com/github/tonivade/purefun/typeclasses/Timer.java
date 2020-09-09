/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.time.Duration;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;

public interface Timer<F extends Witness> {

  Kind<F, Unit> sleep(Duration duration);
  
  Kind<F, Long> currentNanos();
}
