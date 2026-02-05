package com.github.tonivade.purefun.data;

@FunctionalInterface
public interface Reducer<A, E> {

  A apply(A accumulator, E element);

}
