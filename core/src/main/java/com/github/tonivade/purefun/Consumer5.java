/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Unit.unit;

/**
 * <p>This interface represents a function that receives four parameters but it doesn't generate any result.</p>
 * <p>It's like a {@code Function4<A, B, C, D, Unit>}</p>
 * @param <A> the type of first parameter received by the function
 * @param <B> the type of second parameter received by the function
 * @param <C> the type of third parameter received by the function
 * @param <D> the type of fourth parameter received by the function
 */
@FunctionalInterface
public interface Consumer5<A, B, C, D, E> extends Recoverable {

  default void accept(A value1, B value2, C value3, D value4, E value5) {
    try {
      run(value1, value2, value3, value4, value5);
    } catch (Throwable t) {
      sneakyThrow(t);
    }
  }

  void run(A value1, B value2, C value3, D value4, E value5) throws Throwable;

  default Consumer5<A, B, C, D, E> andThen(Consumer5<? super A, ? super B, ? super C, ? super D, ? super E> after) {
    return (value1, value2, value3, value4, value5) -> { 
      accept(value1, value2, value3, value4, value5); 
      after.accept(value1, value2, value3, value4, value5); 
    };
  }

  default Function5<A, B, C, D, E, Unit> asFunction() {
    return (value1, value2, value3, value4, value5) -> { 
      accept(value1, value2, value3, value4, value5); 
      return unit(); 
      };
  }

  @SuppressWarnings("unchecked")
  static <A, B, C, D, E> Consumer5<A, B, C, D, E> of(Consumer5<? super A, ? super B, ? super C, ? super D, ? super E> reference) {
    return (Consumer5<A, B, C, D, E>) reference;
  }
}
