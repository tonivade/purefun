package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Kind;

@FunctionalInterface
public interface Pure<F extends Kind<F, ?>> {

  <T> Kind<F, T> apply(T value);

}
