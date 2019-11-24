/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

class Environment {

  private final int value;

  Environment(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
