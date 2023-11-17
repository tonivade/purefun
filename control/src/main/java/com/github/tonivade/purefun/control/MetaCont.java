/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.NonEmptyList;
import com.github.tonivade.purefun.type.Option;

sealed interface MetaCont<A, B> {

  Result<B> apply(A value);

  <C> MetaCont<A, C> append(MetaCont<B, C> next);

  <R> Tuple2<MetaCont<A, R>, MetaCont<R, B>> splitAt(Marker.Cont<R> cont);

  default <R> MetaCont<R, B> map(Function1<? super R, ? extends A> mapper) {
    return flatMap(x -> Control.later(() -> mapper.apply(x)));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  default <R> MetaCont<R, B> flatMap(Function1<? super R, ? extends Kind<Control_, ? extends A>> mapper) {
    Function1<?, Control<?>> f = (Function1) mapper;
    return new Frames<>(NonEmptyList.of(f), this);
  }

  Result<B> unwind(Throwable throwable);

  static <A> MetaCont<A, A> returnCont() {
    return new Return<>();
  }

  static <A, B, S> MetaCont<A, B> stateCont(Marker.State<S> marker, MetaCont<A, B> tail) {
    return new State<>(marker, tail);
  }

  static <A, B> MetaCont<A, B> handlerCont(Marker.Cont<A> marker, MetaCont<A, B> tail) {
    return new Handler<>(marker, tail);
  }

  static <A, B> Catch<A, B> catchCont(Marker.Catch<A> marker, MetaCont<A, B> tail) {
    return new Catch<>(marker, tail);
  }

  final class Return<A> implements MetaCont<A, A>, Recoverable {

    private Return() {}

    @Override
    public Result<A> apply(A value) {
      return Result.value(value);
    }

    @Override
    public <C> MetaCont<A, C> append(MetaCont<A, C> next) {
      return next;
    }

    @Override
    public <R> Tuple2<MetaCont<A, R>, MetaCont<R, A>> splitAt(Marker.Cont<R> cont) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      return sneakyThrow(throwable);
    }

    @Override
    public String toString() {
      return "[]";
    }
  }

  final class Frames<A, B, C> implements MetaCont<A, C> {

    private final NonEmptyList<Function1<?, Control<?>>> frames;
    private final MetaCont<B, C> tail;

    private Frames(NonEmptyList<Function1<?, Control<?>>> frames, MetaCont<B, C> tail) {
      this.frames = checkNonNull(frames);
      this.tail = checkNonNull(tail);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result<C> apply(A value) {
      Option<Function1<?, Control<?>>> head = frames.head();
      ImmutableList<Function1<?, Control<?>>> rest = frames.tail();
      Function1<A, Control<B>> result = (Function1) head.getOrElseThrow();
      if (rest.isEmpty()) {
        return Result.computation(result.apply(value), tail);
      }
      return Result.computation(result.apply(value), new Frames<>(NonEmptyList.of(rest), tail));
    }

    @Override
    public <D> MetaCont<A, D> append(MetaCont<C, D> next) {
      return new Frames<>(frames, tail.append(next));
    }

    @Override
    public <R> Tuple2<MetaCont<A, R>, MetaCont<R, C>> splitAt(Marker.Cont<R> cont) {
      return tail.splitAt(cont).applyTo((h, t) -> Tuple2.of(new Frames<A, B, R>(frames, h), t));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> MetaCont<R, C> flatMap(Function1<? super R, ? extends Kind<Control_, ? extends A>> mapper) {
      NonEmptyList<Function1<?, Control<?>>> list = NonEmptyList.of((Function1) mapper);
      return new Frames<>(list.appendAll(frames), tail);
    }

    @Override
    public Result<C> unwind(Throwable throwable) {
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("frames :: %s", tail);
    }
  }

  final class Handler<R, A> implements MetaCont<R, A> {

    private final Marker.Cont<R> marker;
    private final MetaCont<R, A> tail;

    private Handler(Marker.Cont<R> marker, MetaCont<R, A> tail) {
      this.marker = checkNonNull(marker);
      this.tail = checkNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> MetaCont<R, C> append(MetaCont<A, C> next) {
      return new Handler<>(marker, tail.append(next));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R1> Tuple2<MetaCont<R, R1>, MetaCont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      if (cont.getClass().isAssignableFrom(marker.getClass())) {
        Handler<R, R1> handler = new Handler<>(marker, new Return());
        // XXX: does not match, don't know how it compiles on scala
        Tuple2<MetaCont<R, R1>, MetaCont<R, A>> tuple = Tuple2.of(handler, tail);
        return (Tuple2) tuple;
      }
      return tail.splitAt(cont).applyTo((h, t) -> Tuple2.of(new Handler<R, R1>(marker, h), t));
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("prompt %s :: %s", marker, tail);
    }
  }

  final class State<R, S, A> implements MetaCont<R, A> {

    private final Marker.State<S> marker;
    private final MetaCont<R, A> tail;

    private State(Marker.State<S> marker, MetaCont<R, A> tail) {
      this.marker = checkNonNull(marker);
      this.tail = checkNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> MetaCont<R, C> append(MetaCont<A, C> next) {
      return new State<>(marker, tail.append(next));
    }

    @Override
    public <R1> Tuple2<MetaCont<R, R1>, MetaCont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<MetaCont<R, R1>, MetaCont<R1, A>> tuple = tail.splitAt(cont);
      return tuple.applyTo((h, t) -> Tuple2.of(new Captured<>(marker, marker.backup(), h), t));
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("state %s :: %s", marker, tail);
    }
  }

  final class Catch<R, A> implements MetaCont<R, A> {

    private final Marker.Catch<R> marker;
    private final MetaCont<R, A> tail;

    private Catch(Marker.Catch<R> marker, MetaCont<R, A> tail) {
      this.marker = checkNonNull(marker);
      this.tail = checkNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> MetaCont<R, C> append(MetaCont<A, C> next) {
      return new Catch<>(marker, tail.append(next));
    }

    @Override
    public <R1> Tuple2<MetaCont<R, R1>, MetaCont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<MetaCont<R, R1>, MetaCont<R1, A>> tuple = tail.splitAt(cont);
      return tuple.applyTo((h, t) -> Tuple2.of(new Catch<R, R1>(marker, h), t));
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      if (marker.handle().isDefinedAt(throwable)) {
        return marker.handle().apply(throwable).apply(tail);
      }
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("catch %s :: %s", marker, tail);
    }
  }

  final class Captured<R, S, A> implements MetaCont<R, A> {

    private final Marker.State<S> marker;
    private final S state;
    private final MetaCont<R, A> tail;

    private Captured(Marker.State<S> marker, S state, MetaCont<R, A> tail) {
      this.marker = checkNonNull(marker);
      this.state = checkNonNull(state);
      this.tail = checkNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <C> MetaCont<R, C> append(MetaCont<A, C> next) {
      return new State<>(restore(), tail.append(next));
    }

    @Override
    public <R1> Tuple2<MetaCont<R, R1>, MetaCont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      throw new UnsupportedOperationException();
    }

    private Marker.State<S> restore() {
      marker.restore(state);
      return marker;
    }

    @Override
    public String toString() {
      return "???";
    }
  }
}
