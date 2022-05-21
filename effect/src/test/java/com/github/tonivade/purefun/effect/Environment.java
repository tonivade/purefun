/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

class Environment {

  private final int value;

  Environment(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}
