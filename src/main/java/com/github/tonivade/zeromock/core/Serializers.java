/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.function.Function;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.google.gson.GsonBuilder;

public final class Serializers {
  
  private Serializers() {}
  
  public static <T> Function<T, Bytes> json() {
    return Serializers.<T>asJson().andThen(Bytes::asBytes);
  }
  
  public static <T> Function<T, Bytes> xml() {
    return Serializers.<T>asXml().andThen(Bytes::asBytes);
  }

  public static <T> Function<T, Bytes> plain() {
    return Serializers.<T>asString().andThen(Bytes::asBytes);
  }
  
  private static <T> Function<T, String> asJson() {
    return value -> new GsonBuilder().create().toJson(value);
  }
  
  private static <T> Function<T, String> asXml() {
    return Serializers::toXml;
  }
  
  private static <T> String toXml(T value) {
    try {
      JAXBContext context = JAXBContext.newInstance(value.getClass());
      StringWriter writer = new StringWriter();
      Marshaller marshaller = context.createMarshaller();
      marshaller.marshal(value, writer);
      return writer.toString();
    } catch (JAXBException e) {
      throw new UncheckedIOException(new IOException(e));
    }
  }
  
  private static <T> Function<T, String> asString() {
    return Object::toString;
  }
}
