/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Either;

public interface ZIORuntime<F extends Kind> {

  <R, E, A> Higher1<F, Either<E, A>> run(R env, ZIO<R, E, A> effect);
}
