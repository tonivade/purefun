/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import static com.github.tonivade.purefun.core.Unit.unit;

/**
 * <p>This interface represents a function that receives four parameters but it doesn't generate any result.</p>
 * <p>It's like a {@code Function4<A, B, C, D, Unit>}</p>
 * @param <A> the type of first parameter received by the function
 * @param <B> the type of second parameter received by the function
 * @param <C> the type of third parameter received by the function
 * @param <D> the type of fourth parameter received by the function
 */
@FunctionalInterface
public interface Consumer4<A, B, C, D> extends Recoverable {

  default void accept(A value1, B value2, C value3, D value4) {
    try {
      run(value1, value2, value3, value4);
    } catch (Throwable t) {
      sneakyThrow(t);
    }
  }

  void run(A value1, B value2, C value3, D value4) throws Throwable;

  default Consumer4<A, B, C, D> andThen(Consumer4<? super A, ? super B, ? super C, ? super D> after) {
    return (value1, value2, value3, value4) -> { 
      accept(value1, value2, value3, value4); 
      after.accept(value1, value2, value3, value4); 
    };
  }

  default Function4<A, B, C, D, Unit> asFunction() {
    return (value1, value2, value3, value4) -> { 
      accept(value1, value2, value3, value4); 
      return unit(); 
      };
  }

  @SuppressWarnings("unchecked")
  static <A, B, C, D> Consumer4<A, B, C, D> of(Consumer4<? super A, ? super B, ? super C, ? super D> reference) {
    return (Consumer4<A, B, C, D>) reference;
  }
}
