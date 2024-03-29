/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Unit;

public class UnitTest {

  @Test
  public void onlyOneInstance() {
    assertSame(Unit.unit(), Unit.unit());
  }
  
  @Test
  public void serializable() throws IOException, ClassNotFoundException {
    Unit unit = Unit.unit();
    
    byte[] bytes;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);) {
      out.writeObject(unit);
      out.flush();

      bytes = baos.toByteArray();
    }
    
    Object result;
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
      result = in.readObject();
    }
   
    assertSame(unit, result);
  }
}
