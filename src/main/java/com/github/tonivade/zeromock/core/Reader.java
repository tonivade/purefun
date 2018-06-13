/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.zeromock.core;

import static java.util.Objects.requireNonNull;

public final class Reader<R, A> implements Functor<A> {
  
  private final Handler1<R, A> run;

  private Reader(Handler1<R, A> run) {
    this.run = requireNonNull(run);
  }
  
  @Override
  public <B> Reader<R, B> map(Handler1<A, B> map) {
    return new Reader<>(reader -> map.handle(eval(reader)));
  }
  
  public <B> Reader<R, B> flatMap(Handler1<A, Reader<R, B>> map) {
    return new Reader<>(reader -> map.handle(eval(reader)).eval(reader));
  }
  
  public A eval(R reader) {
    return run.handle(reader);
  }
  
  public static <R, A> Reader<R, A> unit(A value) {
    return reader(reader -> value);
  }
  
  public static <R, A> Reader<R, A> reader(Handler1<R, A> run) {
    return new Reader<>(run);
  }
}
