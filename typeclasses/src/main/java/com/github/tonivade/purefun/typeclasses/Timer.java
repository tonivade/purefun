/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.time.Duration;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Unit;

public interface Timer<F extends Kind<F, ?>> {

  Kind<F, Unit> sleep(Duration duration);

  Kind<F, Long> currentNanos();
}
