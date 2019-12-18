/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;

import java.util.Stack;

import static com.github.tonivade.purefun.Producer.cons;
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
public abstract class Eval<A> {

  public static Eval<Boolean> TRUE = now(true);
  public static Eval<Boolean> FALSE = now(false);
  public static Eval<Unit> UNIT = now(unit());
  public static Eval<Integer> ZERO = now(0);
  public static Eval<Integer> ONE = now(1);

  private Eval() {}

  public abstract A value();

  public <R> Eval<R> map(Function1<A, R> map) {
    return flatMap(value -> now(map.apply(value)));
  }

  public abstract <R> Eval<R> flatMap(Function1<A, Eval<R>> map);

  protected abstract Eval<A> collapse();

  public static <T> Eval<T> now(T value) {
    return new Done<>(cons(value));
  }

  public static <T> Eval<T> later(Producer<T> later) {
    return new Done<>(later.memoized());
  }

  public static <T> Eval<T> always(Producer<T> always) {
    return new Done<>(always);
  }

  public static <T> Eval<T> defer(Producer<Eval<T>> eval) {
    return new Defer<>(eval);
  }

  private static final class Done<A> extends Eval<A> {

    private final Producer<A> producer;

    private Done(Producer<A> producer) {
      this.producer = requireNonNull(producer);
    }

    @Override
    public A value() {
      return producer.get();
    }

    public <R> Eval<R> flatMap(Function1<A, Eval<R>> map) {
      return new FlatMapped<>(cons(this), map::apply);
    }

    protected Eval<A> collapse() {
      return this;
    }
  }

  private static final class Defer<A> extends Eval<A> {

    private final Producer<Eval<A>> defer;

    private Defer(Producer<Eval<A>> defer) {
      this.defer = requireNonNull(defer);
    }

    @Override
    public A value() {
      return collapse().value();
    }

    @Override
    public <R> Eval<R> flatMap(Function1<A, Eval<R>> map) {
      return new FlatMapped<>(defer::get, map::apply);
    }

    @Override
    protected Eval<A> collapse() {
      return defer.get().collapse();
    }
  }

  private static final class FlatMapped<A, B> extends Eval<B> {

    private final Producer<Eval<A>> start;
    private final Function1<A, Eval<B>> run;

    private FlatMapped(Producer<Eval<A>> start, Function1<A, Eval<B>> run) {
      this.start = requireNonNull(start);
      this.run = requireNonNull(run);
    }

    @Override
    public B value() {
      return evaluate(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Eval<R> flatMap(Function1<B, Eval<R>> map) {
      return new FlatMapped<>(() -> (Eval<B>) start(), b -> new FlatMapped<>(() -> run((A) b), map::apply));
    }

    @Override
    protected Eval<B> collapse() {
      return new FlatMapped<>(start, a -> run(a).collapse());
    }

    private Eval<A> start() {
      return start.get();
    }

    private Eval<B> run(A value) {
      return run.apply(value);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static <A> A evaluate(Eval<A> eval) {
    Stack<Function1<Object, Eval>> stack = new Stack<>();
    Eval<A> current = eval;
    while (true) {
      if (current instanceof Eval.FlatMapped) {
        FlatMapped currentFlatMapped = (FlatMapped) current;
        Eval<A> next = currentFlatMapped.start();
        if (next instanceof Eval.FlatMapped) {
          FlatMapped nextFlatMapped = (FlatMapped) next;
          current = nextFlatMapped.start();
          stack.push(currentFlatMapped::run);
          stack.push(nextFlatMapped::run);
        } else {
          current = (Eval<A>) currentFlatMapped.run(next.value());
        }
      } else if (!stack.isEmpty()) {
        current = (Eval<A>) stack.pop().apply(current.value());
      } else {
        break;
      }
    }
    return current.value();
  }
}
