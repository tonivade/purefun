/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.nio.ByteBuffer.wrap;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static tonivade.equalizer.Equalizer.equalizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class Bytes {

  private static final int BUFFER_SIZE = 1024;
  
  private final byte[] buffer;

  private Bytes(byte[] buffer) {
    this.buffer = requireNonNull(buffer);
  }

  public byte[] toArray() {
    return buffer;
  }

  public ByteBuffer getBuffer() {
    return wrap(buffer);
  }

  public int size() {
    return buffer.length;
  }
  
  public boolean isEmpty() {
    return size() == 0;
  }
  
  @Override
  public int hashCode() {
    return Arrays.hashCode(buffer);
  }
  
  @Override
  public boolean equals(Object obj) {
    return equalizer(this)
        .append((a, b) -> Arrays.equals(a.buffer, b.buffer))
        .applyTo(obj);
  }

  public static Bytes empty() {
    return new Bytes(new byte[]{});
  }
  
  public static Bytes fromArray(byte[] array) {
    return new Bytes(array);
  }

  public static Bytes asBytes(InputStream input) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[BUFFER_SIZE];
    while (true) {
      int read = input.read(buffer);
      if (read > 0) {
        out.write(buffer, 0, read);
      } else break;
    }
    return new Bytes(out.toByteArray());
  }
  
  public static Bytes asBytes(String string) {
    return new Bytes(string.getBytes(UTF_8));
  }
  
  public static String asString(Bytes buffer) {
    return new String(buffer.toArray(), UTF_8);
  }
}
