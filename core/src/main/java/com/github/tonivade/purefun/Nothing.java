/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

/**
 * It represents a type that cannot be instantiated. Similar to {@link Void} type
 * in JVM but is not a substitution for {@code void}
 */
public final class Nothing {

  private Nothing() {}

  public static Nothing nothing() {
    return null;
  }
}
