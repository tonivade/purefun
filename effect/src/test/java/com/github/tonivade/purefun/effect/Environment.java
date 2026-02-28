/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
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
