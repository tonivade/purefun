/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Sealed;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.NonEmptyList;
import com.github.tonivade.purefun.type.Option;

import static java.util.Objects.requireNonNull;

@Sealed
@HigherKind
public interface Control<T> {

  <R> Result<R> apply(Cont<T, R> cont);

  default T run() {
    return Result.trampoline(apply(new Cont.Return<>()));
  }

  default <R> Control<R> map(Function1<T, R> mapper) {
    return new Control<R>() {
      @Override
      public <R1> Result<R1> apply(Cont<R, R1> cont) {
        return Control.this.apply(cont.map(mapper));
      }
    };
  }

  default <R> Control<R> flatMap(Function1<T, Control<R>> mapper) {
    return new Control<R>() {
      @Override
      public <R1> Result<R1> apply(Cont<R, R1> cont) {
        return Control.this.apply(cont.flatMap(mapper));
      }
    };
  }

  default <R> Control<R> andThen(Control<R> next) {
    return flatMap(ignore -> next);
  }

  final class Use<A, R> implements Control<A> {

    private final Marker.Cont<R> marker;
    private final Function1<Function1<A, Control<R>>, Control<R>> cps;

    public Use(Marker.Cont<R> marker, Function1<Function1<A, Control<R>>, Control<R>> cps) {
      this.marker = requireNonNull(marker);
      this.cps = requireNonNull(cps);
    }

//    private[effekt] final def use[A, Res](c: ContMarker[Res])(f: CPS[A, Res]): Control[A] =
//        new Control[A] {
//      def apply[R](k: MetaCont[A, R]): Result[R] = {
//
//          val (init, tail) = k splitAt c
//
//          val handled: Control[Res] = f { a =>
//        new Control[Res] {
//          def apply[R2](k: MetaCont[Res, R2]): Result[R2] =
//              (init append k).apply(a)
//        }
//      }
//
//      // continue with tail
//      Computation(handled, tail)
//      }
//    }
    @Override
    public <R1> Result<R1> apply(Cont<A, R1> cont) {
      Tuple2<Cont<A, R>, Cont<R1, R>> tuple = cont.splitAt(marker);
      Control<R> handled = cps.apply(a -> new Control<R>() {
        @Override
        public <R2> Result<R2> apply(Cont<R, R2> cont) {
          return tuple.get1().append(cont).apply(a);
        }
      });
      Cont<R1, R> tuple2 = tuple.get2();
      // new Result.Computation<>(handled, tuple2);
      return null;
    }
  }

  final class DelimitCont<R> {
    // TODO
    //  type using[+A, -E] = E => Control[A]
    //  def delimitCont[Res](marker: ContMarker[Res])(f: Res using marker.type): Control[Res] ???
  }

  final class DelimitState<R, S> implements Control<R> {

    private final Marker.State<S> marker;
    private final Control<R> control;

    public DelimitState(Marker.State<S> marker, Control<R> control) {
      this.marker = requireNonNull(marker);
      this.control = requireNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(Cont<R, R1> cont) {
      return new Result.Computation<>(control, new Cont.State<>(marker, cont));
    }
  }

  final class DelimitCatch<R> implements Control<R> {

    private final Marker.Catch<R> marker;
    private final Control<R> control;

    public DelimitCatch(Marker.Catch<R> marker, Control<R> control) {
      this.marker = requireNonNull(marker);
      this.control = requireNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(Cont<R, R1> cont) {
      return new Result.Computation<>(control, new Cont.Catch<>(marker, cont));
    }
  }

  final class Pure<T> implements Control<T> {

    private final T value;

    public Pure(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public T run() {
      return value;
    }

    @Override
    public <R> Result<R> apply(Cont<T, R> cont) {
      return cont.apply(value);
    }

    @Override
    public <R> Control<R> map(Function1<T, R> mapper) {
      return new Pure<>(mapper.apply(value));
    }
  }

  @SuppressWarnings("unchecked")
  final class Failure<T> implements Control<T> {

    private final Throwable error;

    public Failure(Throwable error) {
      this.error = requireNonNull(error);
    }

    @Override
    public <R> Result<R> apply(Cont<T, R> cont) {
      return new Result.Abort<>(error);
    }

    @Override
    public <R> Control<R> map(Function1<T, R> mapper) {
      return (Control<R>) this;
    }

    @Override
    public <R> Control<R> flatMap(Function1<T, Control<R>> mapper) {
      return (Control<R>) this;
    }
  }
}

interface Result<T> {

  @SuppressWarnings({"unchecked", "rawtypes"})
  static <T> T trampoline(Result<T> apply) {
    Result<T> result = apply;

    while (result.isComputation()) {
      Computation comp = (Computation) result;

      try {
        result = (Result<T>) comp.control.apply(comp.continuation);
      } catch (Throwable t) {
        result = comp.continuation.unwind(t);
      }
    }

    return result.value();
  }

  T value();

  default boolean isComputation() {
    return false;
  }

  final class Value<T> implements Result<T> {

    private final T value;

    public Value(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public T value() {
      return value;
    }

    @Override
    public String toString() {
      return String.format("Value(%s)", value);
    }
  }

  final class Abort<T> implements Result<T>, Recoverable {

    private final Throwable error;

    public Abort(Throwable error) {
      this.error = requireNonNull(error);
    }

    @Override
    public T value() {
      return sneakyThrow(error);
    }

    @Override
    public String toString() {
      return String.format("Error(%s)", error);
    }
  }

  final class Computation<T, R> implements Result<T> {

    private final Control<R> control;
    private final Cont<R, T> continuation;

    public Computation(Control<R> control, Cont<R, T> continuation) {
      this.control = requireNonNull(control);
      this.continuation = requireNonNull(continuation);
    }

    @Override
    public boolean isComputation() {
      return true;
    }

    @Override
    public T value() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return String.format("Computation(%s, %s)", control, continuation);
    }
  }
}

interface Cont<A, B> {

  Result<B> apply(A value);

  <C> Cont<A, C> append(Cont<B, C> next);

  <R> Tuple2<Cont<A, R>, Cont<B, R>> splitAt(Marker.Cont<R> cont);

  default <R> Cont<R, B> map(Function1<R, A> mapper) {
    return flatMap(x -> new Control.Pure<>(mapper.apply(x)));
  }

  @SuppressWarnings("unchecked")
  default <R> Cont<R, B> flatMap(Function1<R, Control<A>> mapper) {
    Function1<?, Control<?>> x = Function1.class.cast(mapper);
    return new Frames<>(NonEmptyList.of(x), this);
  }

  Result<B> unwind(Throwable effect);

  final class Return<A> implements Cont<A, A>, Recoverable {

    @Override
    public Result<A> apply(A value) {
      return new Result.Value<>(value);
    }

    @Override
    public <C> Cont<A, C> append(Cont<A, C> next) {
      return next;
    }

    @Override
    public <R> Tuple2<Cont<A, R>, Cont<A, R>> splitAt(Marker.Cont<R> cont) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Result<A> unwind(Throwable effect) {
      return sneakyThrow(effect);
    }

    @Override
    public String toString() {
      return "[]";
    }
  }

  final class Frames<A, B, C> implements Cont<A, C> {

    private final NonEmptyList<Function1<?, Control<?>>> frames;
    private final Cont<B, C> tail;

    public Frames(NonEmptyList<Function1<?, Control<?>>> frames, Cont<B, C> tail) {
      this.frames = requireNonNull(frames);
      this.tail = requireNonNull(tail);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result<C> apply(A value) {
      Option<Function1<?, Control<?>>> head = frames.head();
      ImmutableList<Function1<?, Control<?>>> rest = frames.tail();
      Function1<A, Control<B>> result = Function1.class.cast(head.get());
      if (rest.isEmpty()) {
        return new Result.Computation<>(result.apply(value), tail);
      }
      return new Result.Computation<>(result.apply(value), new Frames<>(NonEmptyList.of(rest), tail));
    }

    @Override
    public <D> Cont<A, D> append(Cont<C, D> next) {
      return new Frames<>(frames, tail.append(next));
    }

    @Override
    public <R> Tuple2<Cont<A, R>, Cont<C, R>> splitAt(Marker.Cont<R> cont) {
      return tail.splitAt(cont).applyTo((head, tail) -> Tuple2.of(new Frames<>(frames, head), tail));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Cont<R, C> flatMap(Function1<R, Control<A>> mapper) {
      Function1<?, Control<?>> x = Function1.class.cast(mapper);
      NonEmptyList<Function1<?, Control<?>>> of = NonEmptyList.of(x);
      NonEmptyList<Function1<?, Control<?>>> append = of.appendAll(frames);
      return new Frames<>(append, tail);
    }

    @Override
    public Result<C> unwind(Throwable effect) {
      return tail.unwind(effect);
    }

    @Override
    public String toString() {
      return String.format("fs :: %s", tail);
    }
  }

  final class Handler<R, A> implements Cont<R, A> {

    private final Marker.Cont<R> marker;
    private final Cont<R, A> tail;

    public Handler(Marker.Cont<R> marker, Cont<R, A> tail) {
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
    public <R1> Tuple2<Cont<R, R1>, Cont<A, R1>> splitAt(Marker.Cont<R1> cont) {
      // TODO
      //     // Here we deduce type equality from referential equality
      //    case _: marker.type => (HandlerCont(marker)(ReturnCont()), tail)
      //    case _ => tail.splitAt(c) match {
      //      case (head, tail) => (HandlerCont(marker)(head), tail)
      //    }
      return null;
    }

    @Override
    public Result<A> unwind(Throwable effect) {
      return tail.unwind(effect);
    }

    @Override
    public String toString() {
      return String.format("prompt %s :: %s", marker, tail);
    }
  }

  final class State<R, S, A> implements Cont<R, A> {

    private final Marker.State<S> marker;
    private final Cont<R, A> tail;

    public State(Marker.State<S> marker, Cont<R, A> tail) {
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
    public <R1> Tuple2<Cont<R, R1>, Cont<A, R1>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<Cont<R, R1>, Cont<A, R1>> tuple = tail.splitAt(cont);
      return tuple.applyTo((head, tail) -> Tuple2.of(new Captured<>(marker, marker.backup(), head), tail));
    }

    @Override
    public Result<A> unwind(Throwable effect) {
      return tail.unwind(effect);
    }

    @Override
    public String toString() {
      return String.format("state %s :: %s", marker, tail);
    }
  }

  final class Catch<R, A> implements Cont<R, A> {

    private final Marker.Catch<R> marker;
    private final Cont<R, A> tail;

    public Catch(Marker.Catch<R> marker, Cont<R, A> tail) {
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
    public <R1> Tuple2<Cont<R, R1>, Cont<A, R1>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<Cont<R, R1>, Cont<A, R1>> tuple = tail.splitAt(cont);
      return tuple.applyTo((head, tail) -> Tuple2.of(new Catch<>(marker, head), tail));
    }

    @Override
    public Result<A> unwind(Throwable effect) {
      if (marker.handle().isDefinedAt(effect)) {
        return marker.handle().apply(effect).apply(tail);
      }
      return tail.unwind(effect);
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

    public Captured(Marker.State<S> marker, S state, Cont<R, A> tail) {
      this.marker = marker;
      this.state = state;
      this.tail = tail;
    }

    @Override
    public Result<A> apply(R value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new State<>(marker.restore(state), tail.append(next));
    }

    @Override
    public <R1> Tuple2<Cont<R, R1>, Cont<A, R1>> splitAt(Marker.Cont<R1> cont) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Result<A> unwind(Throwable effect) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
      return "???";
    }
  }
}

interface Marker {

  interface Cont<T> {}

  interface State<T> {
    T backup();
    State<T> restore(T value);
  }

  interface Catch<T> {
    PartialFunction1<Throwable, Control<T>> handle();
  }
}
