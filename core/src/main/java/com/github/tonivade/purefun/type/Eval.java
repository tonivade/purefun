/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;
import static com.github.tonivade.purefun.core.Producer.failure;
import static com.github.tonivade.purefun.core.Unit.unit;

import java.util.ArrayDeque;
import java.util.Deque;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;

/**
 * <p>This is a monad that allows to control the evaluation of a computation or a value.</p>
 * <p>There are 3 basic strategies:</p>
 * <ul>
 *   <li>Eval.now(): evaluated immediately</li>
 *   <li>Eval.later(): the computation is evaluated later, but only the first time, the result is memoized.</li>
 *   <li>Eval.always(): the computation is evaluated later, but is always executed.</li>
 * </ul>
 * @param <A> result of the computation
 */
@HigherKind
public sealed interface Eval<A> extends EvalOf<A>, Bindable<Eval<?>, A> {

  Eval<Boolean> TRUE = now(true);
  Eval<Boolean> FALSE = now(false);
  Eval<Unit> UNIT = now(unit());
  Eval<Integer> ZERO = now(0);
  Eval<Integer> ONE = now(1);

  A value();

  @Override
  default <R> Eval<R> map(Function1<? super A, ? extends R> map) {
    return flatMap(value -> now(map.apply(value)));
  }

  @Override
  <R> Eval<R> flatMap(Function1<? super A, ? extends Kind<Eval<?>, ? extends R>> map);

  static <T> Eval<T> now(T value) {
    return new Done<>(cons(value));
  }

  static <T> Eval<T> later(Producer<? extends T> later) {
    return new Done<>(later.memoized());
  }

  static <T> Eval<T> always(Producer<? extends T> always) {
    return new Done<>(always);
  }

  static <T> Eval<T> defer(Producer<? extends Kind<Eval<?>, ? extends T>> eval) {
    return new Defer<>(eval);
  }

  static <T> Eval<T> raiseError(Throwable error) {
    return new Done<>(failure(cons(error)));
  }

  final class Done<A> implements Eval<A> {

    private final Producer<? extends A> producer;

    private Done(Producer<? extends A> producer) {
      this.producer = checkNonNull(producer);
    }

    @Override
    public A value() {
      return producer.get();
    }

    @Override
    public <R> Eval<R> flatMap(Function1<? super A, ? extends Kind<Eval<?>, ? extends R>> map) {
      return new FlatMapped<>(cons(this), map::apply);
    }

    @Override
    public String toString() {
      return "Done(?)";
    }
  }

  final class Defer<A> implements Eval<A> {

    private final Producer<? extends Kind<Eval<?>, ? extends A>> deferred;

    private Defer(Producer<? extends Kind<Eval<?>, ? extends A>> deferred) {
      this.deferred = checkNonNull(deferred);
    }

    @Override
    public A value() {
      return collapse(this).value();
    }

    @Override
    public <R> Eval<R> flatMap(Function1<? super A, ? extends Kind<Eval<?>, ? extends R>> map) {
      return new FlatMapped<>(deferred::get, map::apply);
    }

    @SuppressWarnings("unchecked")
    private Eval<A> next() {
      return (Eval<A>) deferred.get();
    }

    @Override
    public String toString() {
      return "Defer(?)";
    }
  }

  final class FlatMapped<A, B> implements Eval<B> {

    private final Producer<? extends Kind<Eval<?>, ? extends A>> start;
    private final Function1<? super A, ? extends Kind<Eval<?>, ? extends B>> run;

    private FlatMapped(Producer<? extends Kind<Eval<?>, ? extends A>> start, Function1<? super A, ? extends Kind<Eval<?>, ? extends B>> run) {
      this.start = checkNonNull(start);
      this.run = checkNonNull(run);
    }

    @Override
    public B value() {
      return evaluate(this);
    }

    @Override
    public <R> Eval<R> flatMap(Function1<? super B, ? extends Kind<Eval<?>, ? extends R>> map) {
      return new FlatMapped<>(this::start, b -> new FlatMapped<>(() -> run(b), map::apply));
    }

    private Eval<A> start() {
      return EvalOf.narrowK(start.get());
    }

    private Eval<B> run(A value) {
      Function1<? super A, Eval<B>> andThen = run.andThen(EvalOf::narrowK);
      return andThen.apply(value);
    }

    @Override
    public String toString() {
      return "FlatMapped(?, ?)";
    }
  }

  @SuppressWarnings("unchecked")
  private static <A, X> Eval<A> collapse(Eval<A> self) {
    Eval<A> current = self;
    while (true) {
      if (current instanceof Eval.Defer<A> defer) {
        current = defer.next();
      } else if (current instanceof Eval.FlatMapped) {
        Eval.FlatMapped<X, A> flatMapped = (Eval.FlatMapped<X, A>) current;
        return new Eval.FlatMapped<>(flatMapped::start, a -> collapse(flatMapped.run(a)));
      } else {
        break;
      }
    }
    return current;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static <A> A evaluate(Eval<A> self) {
    Deque<Function1<Object, Eval>> stack = new ArrayDeque<>();
    Eval<A> current = self;
    while (true) {
      if (current instanceof Eval.FlatMapped currentFlatMapped) {
        Eval<A> next = currentFlatMapped.start();
        if (next instanceof Eval.FlatMapped nextFlatMapped) {
          current = nextFlatMapped.start();
          stack.push(currentFlatMapped::run);
          stack.push(nextFlatMapped::run);
        } else {
          current = currentFlatMapped.run(next.value());
        }
      } else if (!stack.isEmpty()) {
        current = stack.pop().apply(current.value());
      } else {
        break;
      }
    }
    return current.value();
  }
}
