/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

public final class Nothing {
  
  private static final Nothing INSTANCE = new Nothing();
  
  private Nothing() {}
  
  public static Nothing nothing() {
    return INSTANCE;
  }
  
  @Override
  public String toString() {
    return "Nothing";
  }
}
