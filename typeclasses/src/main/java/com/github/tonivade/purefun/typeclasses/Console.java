/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.NonEmptyList;

public interface Console<F extends Kind<F, ?>> {

  Kind<F, String> readln();

  Kind<F, Unit> println(String text);

  default Kind<F, Unit> println(Object text, Object ... args) {
    return println(NonEmptyList.of(text, args).join(","));
  }

  default Kind<F, Unit> printf(String template, Object...args) {
    return println(String.format(template, args));
  }
}
