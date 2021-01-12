/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.lang.reflect.Type;

public class InstanceNotFoundException extends RuntimeException {
  
  private static final long serialVersionUID = 1L;

  public InstanceNotFoundException(Type kind, Class<?> typeClass, Throwable cause) {
    super("instance of type " + typeClass.getSimpleName() + " for type " + kind.getTypeName() + " not found", cause);
  }
}
