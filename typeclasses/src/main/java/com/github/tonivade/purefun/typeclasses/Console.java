/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.data.NonEmptyList;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.annotation.Witness;

public interface Console<F extends Witness> {

  Kind<F, String> readln();

  Kind<F, Unit> println(String text);
  
  default Kind<F, Unit> println(Object text, Object ... args) {
    return println(NonEmptyList.of(text, args).join(","));
  }
  
  default Kind<F, Unit> printf(String template, Object...args) {
    return println(String.format(template, args));
  }
}
