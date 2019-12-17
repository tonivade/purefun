/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

/**
 * <p>This is a monad that allows to control the evaluation of a computation or a value.</p>
 * <p>There are 3 basic strategies:</p>
 * <ul>
 *   <li>Eval.now(): evaluated immediately</li>
 *   <li>Eval.later(): the computation is evaluated later, but only the first time, the result is memoized.</li>
 *   <li>Eval.always(): the computation is evaluated later, but is always executed.</li>
 * </ul>
 * <p><strong>Warning:</strong> Not stack safe</p>
 * @param <A> result of the computation
 */
@HigherKind
public interface Eval<A> {

  Eval<Boolean> TRUE = now(true);
  Eval<Boolean> FALSE = now(false);
  Eval<Unit> UNIT = now(unit());
  Eval<Integer> ZERO = now(0);
  Eval<Integer> ONE = now(1);

  A value();

  default <R> Eval<R> map(Function1<A, R> map) {
    return flatMap(value -> now(map.apply(value)));
  }

  default <R> Eval<R> flatMap(Function1<A, Eval<R>> map) {
    return new FlatMapped<R>() {
      @Override
      protected <B> Eval<B> start() {
        return (Eval<B>) Eval.this;
      }

      @Override
      protected <B> Eval<R> run(B value) {
        return map.apply((A) value);
      }
    };
  }

  default Eval<A> collapse() {
    return this;
  }

  static <T> Eval<T> now(T value) {
    return new Now<>(value);
  }

  static <T> Eval<T> later(Producer<T> later) {
    return new Later<>(later);
  }

  static <T> Eval<T> always(Producer<T> always) {
    return new Always<>(always);
  }

  static <T> Eval<T> defer(Producer<Eval<T>> eval) {
    return new Defer<>(eval);
  }

  final class Now<A> implements Eval<A> {

    private A value;

    private Now(A value) {
      this.value = requireNonNull(value);
    }

    @Override
    public A value() {
      return value;
    }
  }

  final class Later<A> implements Eval<A> {

    private Producer<A> later;

    private Later(Producer<A> later) {
      this.later = later.memoized();
    }

    @Override
    public A value() {
      return later.get();
    }
  }

  final class Always<A> implements Eval<A> {

    private Producer<A> later;

    private Always(Producer<A> later) {
      this.later = requireNonNull(later);
    }

    @Override
    public A value() {
      return later.get();
    }
  }

  final class Defer<A> implements Eval<A> {

    private Producer<Eval<A>> defer;

    private Defer(Producer<Eval<A>> defer) {
      this.defer = requireNonNull(defer);
    }

    @Override
    public A value() {
      return collapse().value();
    }

    @Override
    public <R> Eval<R> flatMap(Function1<A, Eval<R>> map) {
      return new FlatMapped<R>() {
        @Override
        protected <X> Eval<X> start() {
          return (Eval<X>) defer.get();
        }

        @Override
        protected <X> Eval<R> run(X value) {
          return map.apply((A) value);
        }
      };
    }

    @Override
    public Eval<A> collapse() {
      return defer.get().collapse();
    }
  }

  abstract class FlatMapped<B> implements Eval<B> {

    private FlatMapped() {}

    protected abstract <A> Eval<A> start();
    protected abstract <A> Eval<B> run(A value);

    @Override
    public B value() {
      return run(start().collapse().value()).collapse().value();
    }

    @Override
    public <R> Eval<R> flatMap(Function1<B, Eval<R>> map) {
      return new FlatMapped<R>() {
        @Override
        protected <X> Eval<X> start() {
          return FlatMapped.this.start();
        }

        @Override
        protected <X> Eval<R> run(X value1) {
          return new FlatMapped<R>() {
            @Override
            protected <Y> Eval<Y> start() {
              return (Eval<Y>) FlatMapped.this.run(value1);
            }

            @Override
            protected <Y> Eval<R> run(Y value2) {
              return map.apply((B) value2);
            }
          };
        }
      };
    }

    @Override
    public Eval<B> collapse() {
      return new FlatMapped<B>() {
        @Override
        protected <A> Eval<A> start() {
          return FlatMapped.this.start();
        }

        @Override
        protected <A> Eval<B> run(A value) {
          return FlatMapped.this.run(value).collapse();
        }
      };
    }
  }
}
