/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Sealed;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.NonEmptyList;
import com.github.tonivade.purefun.type.Option;

import static java.util.Objects.requireNonNull;

@Sealed
interface Cont<A, B> {

  Result<B> apply(A value);

  <C> Cont<A, C> append(Cont<B, C> next);

  <R> Tuple2<Cont<A, R>, Cont<R, B>> splitAt(Marker.Cont<R> cont);

  default <R> Cont<R, B> map(Function1<R, A> mapper) {
    return flatMap(x -> Control.later(() -> mapper.apply(x)));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  default <R> Cont<R, B> flatMap(Function1<R, Control<A>> mapper) {
    Function1<?, Control<?>> f = (Function1) mapper;
    return new Frames<>(NonEmptyList.of(f), this);
  }

  Result<B> unwind(Throwable throwable);

  static <A> Cont<A, A> _return() {
    return new Return<>();
  }

  static <A, B, S> Cont<A, B> state(Marker.State<S> marker, Cont<A, B> tail) {
    return new State<>(marker, tail);
  }

  static <A, B> Cont<A, B> handler(Marker.Cont<A> marker, Cont<A, B> tail) {
    return new Handler<>(marker, tail);
  }

  static <A, B> Catch<A, B> _catch(Marker.Catch<A> marker, Cont<A, B> tail) {
    return new Catch<>(marker, tail);
  }

  final class Return<A> implements Cont<A, A>, Recoverable {

    private Return() {}

    @Override
    public Result<A> apply(A value) {
      return Result.value(value);
    }

    @Override
    public <C> Cont<A, C> append(Cont<A, C> next) {
      return next;
    }

    @Override
    public <R> Tuple2<Cont<A, R>, Cont<R, A>> splitAt(Marker.Cont<R> cont) {
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

  final class Frames<A, B, C> implements Cont<A, C> {

    private final NonEmptyList<Function1<?, Control<?>>> frames;
    private final Cont<B, C> tail;

    private Frames(NonEmptyList<Function1<?, Control<?>>> frames, Cont<B, C> tail) {
      this.frames = requireNonNull(frames);
      this.tail = requireNonNull(tail);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result<C> apply(A value) {
      Option<Function1<?, Control<?>>> head = frames.head();
      ImmutableList<Function1<?, Control<?>>> rest = frames.tail();
      Function1<A, Control<B>> result = (Function1) head.get();
      if (rest.isEmpty()) {
        return Result.computation(result.apply(value), tail);
      }
      return Result.computation(result.apply(value), new Frames<>(NonEmptyList.of(rest), tail));
    }

    @Override
    public <D> Cont<A, D> append(Cont<C, D> next) {
      return new Frames<>(frames, tail.append(next));
    }

    @Override
    public <R> Tuple2<Cont<A, R>, Cont<R, C>> splitAt(Marker.Cont<R> cont) {
      return tail.splitAt(cont).applyTo((head, tail) -> Tuple2.of(new Frames<>(frames, head), tail));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> Cont<R, C> flatMap(Function1<R, Control<A>> mapper) {
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

  final class Handler<R, A> implements Cont<R, A> {

    private final Marker.Cont<R> marker;
    private final Cont<R, A> tail;

    private Handler(Marker.Cont<R> marker, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new Handler<>(marker, tail.append(next));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      if (cont.getClass().isAssignableFrom(marker.getClass())) {
        Handler<R, R1> handler = new Handler<>(marker, new Return());
        // XXX: does not match, don't now how it compiles on scala
        Tuple2<Cont<R, R1>, Cont<R, A>> tuple = Tuple2.of(handler, tail);
        return (Tuple2) tuple;
      }
      return tail.splitAt(cont).applyTo((head, tail) -> Tuple2.of(new Handler<>(marker, head), tail));
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

  final class State<R, S, A> implements Cont<R, A> {

    private final Marker.State<S> marker;
    private final Cont<R, A> tail;

    private State(Marker.State<S> marker, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new State<>(marker, tail.append(next));
    }

    @Override
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<Cont<R, R1>, Cont<R1, A>> tuple = tail.splitAt(cont);
      return tuple.applyTo((head, tail) -> Tuple2.of(new Captured<>(marker, marker.backup(), head), tail));
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

  final class Catch<R, A> implements Cont<R, A> {

    private final Marker.Catch<R> marker;
    private final Cont<R, A> tail;

    private Catch(Marker.Catch<R> marker, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new Catch<>(marker, tail.append(next));
    }

    @Override
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<Cont<R, R1>, Cont<R1, A>> tuple = tail.splitAt(cont);
      return tuple.applyTo((head, tail) -> Tuple2.of(new Catch<>(marker, head), tail));
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

  final class Captured<R, S, A> implements Cont<R, A> {

    private final Marker.State<S> marker;
    private final S state;
    private final Cont<R, A> tail;

    private Captured(Marker.State<S> marker, S state, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.state = requireNonNull(state);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new State<>(restore(), tail.append(next));
    }

    @Override
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
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
