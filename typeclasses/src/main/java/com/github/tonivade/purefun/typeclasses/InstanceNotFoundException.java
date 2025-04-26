/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.io.Serial;
import java.lang.reflect.Type;

public class InstanceNotFoundException extends RuntimeException {
  
  @Serial
  private static final long serialVersionUID = 1L;

  public InstanceNotFoundException(Type kind, Class<?> typeClass, Throwable cause) {
    super("instance of type " + typeClass.getSimpleName() + " for type " + kind.getTypeName() + " not found", cause);
  }
}
