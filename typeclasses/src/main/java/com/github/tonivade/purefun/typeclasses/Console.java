/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.TypeClass;
import com.github.tonivade.purefun.Unit;

@TypeClass
public interface Console<F extends Kind> {

  Higher1<F, String> readln();

  Higher1<F, Unit> println(String text);
}
