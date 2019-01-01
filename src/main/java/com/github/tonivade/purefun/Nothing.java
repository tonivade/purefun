/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.io.Serializable;

public final class Nothing implements Serializable {

  private static final long serialVersionUID = 7806772464602177670L;

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
