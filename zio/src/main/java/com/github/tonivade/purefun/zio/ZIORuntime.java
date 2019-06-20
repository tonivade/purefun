package com.github.tonivade.purefun.zio;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Either;

public interface ZIORuntime<F extends Kind> {

  <R, E, A> Higher1<F, Either<E, A>> run(R env, ZIO<R, E, A> effect);
}
