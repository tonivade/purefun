/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

/**
 * Type that represents a value that cannot be instantiable. Similar to {@code Void} type.
 */
public final class Nothing {

  private Nothing() {}

  public static Nothing nothing() {
    return null;
  }
}
