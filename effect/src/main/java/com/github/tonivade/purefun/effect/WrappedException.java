/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

class WrappedException extends RuntimeException {
  
  private static final long serialVersionUID = 1L;

  private final Object wrapped;

  private WrappedException(Object wrapped) {
    super(wrapped.toString());
    this.wrapped = checkNonNull(wrapped);
  }
  
  protected static <E> Throwable wrap(E error) {
    if (error instanceof Throwable) {
      return (Throwable) error;
    }
    return new WrappedException(error);
  }
  
  @SuppressWarnings("unchecked")
  protected static <E> E unwrap(Throwable exception) {
    if (exception instanceof WrappedException) {
      return (E) ((WrappedException) exception).wrapped;
    }
    throw new ClassCastException("not compatible: " + exception.getClass());
  }
}
