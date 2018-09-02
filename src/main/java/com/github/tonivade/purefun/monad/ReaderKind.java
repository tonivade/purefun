/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.Higher2;

final class ReaderKind<R, A> implements Reader<R, A>, Higher2<ReaderKind.µ, R, A>{

  public static final class µ {}

  private final Reader<R, A> delegate;

  public ReaderKind(Reader<R, A> delegate) {
    this.delegate = requireNonNull(delegate);
  }

  @Override
  public A eval(R reader) {
    return delegate.eval(reader);
  }

  public static <R, A> Reader<R, A> narrowK(Higher2<ReaderKind.µ, R, A> hkt) {
    return (Reader<R, A>) hkt;
  }
}
