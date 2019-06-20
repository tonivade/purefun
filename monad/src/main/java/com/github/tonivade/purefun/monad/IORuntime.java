package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface IORuntime<F extends Kind> {

  <A> Higher1<F, A> run(IO<A> effect);
}
