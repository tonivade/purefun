/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Producer.cons;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;

@HigherKind
public interface Control<T> extends ControlOf<T> {

  <R> Result<R> apply(MetaCont<T, R> cont);

  default T run() {
    return Result.trampoline(apply(MetaCont.returnCont()));
  }

  default <R> Control<R> map(Function1<? super T, ? extends R> mapper) {
    return new Control<R>() {
      @Override
      public <R1> Result<R1> apply(MetaCont<R, R1> cont) {
        return Control.this.apply(cont.map(mapper));
      }
    };
  }

  default <R> Control<R> flatMap(Function1<? super T, ? extends Control<? extends R>> mapper) {
    return new Control<R>() {
      @Override
      public <R1> Result<R1> apply(MetaCont<R, R1> cont) {
        return Control.this.apply(cont.flatMap(mapper));
      }
    };
  }

  default <R> Control<R> andThen(Control<? extends R> next) {
    return flatMap(ignore -> next);
  }

  static <T> Control<T> pure(T value) {
    return later(cons(value));
  }

  static <T> Control<T> later(Producer<T> value) {
    return new Pure<>(value);
  }

  static <T> Control<T> failure(Throwable throwable) {
    return new Failure<>(throwable);
  }

  static <T, R> Control<T> use(Marker.Cont<R> marker, Function1<Function1<T, Control<R>>, Control<R>> cps) {
    return new Use<>(marker, cps);
  }

  static <R> Control<R> delimitCont(Marker.Cont<R> marker, Function1<? super Marker.Cont<R>, Control<R>> control) {
    return new DelimitCont<>(marker, control);
  }

  static <R, S> Control<R> delimitState(Marker.State<S> marker, Control<R> control) {
    return new DelimitState<>(marker, control);
  }

  static <R> Control<R> delimitCatch(Marker.Catch<R> marker, Control<R> control) {
    return new DelimitCatch<>(marker, control);
  }

  final class Use<A, R> implements Control<A> {

    private final Marker.Cont<R> marker;
    private final Function1<Function1<A, Control<R>>, Control<R>> cps;

    private Use(Marker.Cont<R> marker, Function1<Function1<A, Control<R>>, Control<R>> cps) {
      this.marker = checkNonNull(marker);
      this.cps = checkNonNull(cps);
    }

    @Override
    public <R1> Result<R1> apply(MetaCont<A, R1> cont) {
      Tuple2<MetaCont<A, R>, MetaCont<R, R1>> tuple = cont.splitAt(marker);
      Control<R> handled = cps.apply(value -> new Control<R>() {
        @Override
        public <R2> Result<R2> apply(MetaCont<R, R2> other) {
          return tuple.get1().append(other).apply(value);
        }
      });
      return Result.computation(handled, tuple.get2());
    }
  }

  final class DelimitCont<R> implements Control<R> {

    private final Marker.Cont<R> marker;
    private final Function1<? super Marker.Cont<R>, Control<R>> control;

    private DelimitCont(Marker.Cont<R> marker, Function1<? super Marker.Cont<R>, Control<R>> control) {
      this.marker = checkNonNull(marker);
      this.control = checkNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(MetaCont<R, R1> cont) {
      return Result.computation(control.apply(marker), MetaCont.handlerCont(marker, cont));
    }
  }

  final class DelimitState<R, S> implements Control<R> {

    private final Marker.State<S> marker;
    private final Control<R> control;

    private DelimitState(Marker.State<S> marker, Control<R> control) {
      this.marker = checkNonNull(marker);
      this.control = checkNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(MetaCont<R, R1> cont) {
      return Result.computation(control, MetaCont.stateCont(marker, cont));
    }
  }

  final class DelimitCatch<R> implements Control<R> {

    private final Marker.Catch<R> marker;
    private final Control<R> control;

    private DelimitCatch(Marker.Catch<R> marker, Control<R> control) {
      this.marker = checkNonNull(marker);
      this.control = checkNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(MetaCont<R, R1> cont) {
      return Result.computation(control, MetaCont.catchCont(marker, cont));
    }
  }

  final class Pure<T> implements Control<T> {

    private final Producer<T> value;

    private Pure(Producer<T> value) {
      this.value = checkNonNull(value);
    }

    @Override
    public T run() {
      return value.get();
    }

    @Override
    public <R> Result<R> apply(MetaCont<T, R> cont) {
      return cont.apply(value.get());
    }

    @Override
    public <R> Control<R> map(Function1<? super T, ? extends R> mapper) {
      return new Pure<>(() -> mapper.apply(value.get()));
    }
  }

  final class Failure<T> implements Control<T> {

    private final Throwable error;

    private Failure(Throwable error) {
      this.error = checkNonNull(error);
    }

    @Override
    public <R> Result<R> apply(MetaCont<T, R> cont) {
      return Result.abort(error);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Control<R> map(Function1<? super T, ? extends R> mapper) {
      return (Control<R>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Control<R> flatMap(Function1<? super T, ? extends Control<? extends R>> mapper) {
      return (Control<R>) this;
    }
  }
}

