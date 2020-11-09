/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

public class InstanceNotFoundException extends RuntimeException {
  
  private static final long serialVersionUID = 1L;

  public InstanceNotFoundException(Class<?> kind, String typeClass, Throwable cause) {
    super("instance of type " + typeClass + " for type " + kind.getSimpleName(), cause);
  }
}
