package com.github.tonivade.purefun;

public interface PartialFunction1<T, R> extends Function1<T, R> {

  boolean isDefined(T value);
}
