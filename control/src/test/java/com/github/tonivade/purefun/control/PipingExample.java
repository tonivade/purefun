/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import static com.github.tonivade.purefun.control.ConsOf.toCons;
import static com.github.tonivade.purefun.control.ProdOf.toProd;
import static com.github.tonivade.purefun.core.Unit.unit;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.runtimes.ConsoleExecutor;

class PipingExample {

  interface Send {
    Control<Unit> send(int n);
  }

  interface Receive {
    Control<Integer> receive();
  }

  private Control<Unit> producer(Send send) {
    return send.send(1)
        .andThen(send.send(2))
        .andThen(send.send(3));
  }

  private Control<Unit> consumer(Receive receive) {
    return receive.receive().flatMap(this::println)
        .andThen(receive.receive().flatMap(this::println))
        .andThen(receive.receive().flatMap(this::println));
  }

  private <R> Control<R> pipe(Function1<Receive, Control<R>> down, Function1<Send, Control<R>> up) {
    return down(new Prod<Control<R>>(unit -> cons ->
        up(cons.fix(toCons())).apply(up::apply))).apply(down::apply);
  }

  @Test
  void program() {
    ConsoleExecutor executor = new ConsoleExecutor();

    Producer<Unit> program = () -> pipe(this::consumer, this::producer).run();
    executor.run(program);

    assertEquals("1\n2\n3\n", executor.getOutput());
  }

  private <R> Down<R> down(Prod<Control<R>> prod) {
    return new Down<>(prod);
  }

  private <R> Up<R> up(Cons<Control<R>> cons) {
    return new Up<>(cons);
  }

  private Control<Unit> println(Integer x) {
    return Control.later(() -> { System.out.println(x); return unit(); });
  }

  static abstract class Process<R, P extends Witness, E> extends Stateful<R, Kind<P, Control<R>>, E> {

    Process(Kind<P, Control<R>> init) {
      super(init);
    }
  }

  static final class Down<R> extends Process<R, Prod_, Receive> implements Receive {

    Down(Kind<Prod_, Control<R>> init) {
      super(init);
    }

    @Override
    public Receive effect() { return this; }

    @Override
    public Control<Integer> receive() {
      return useState(state -> resume -> state.fix(toProd()).apply(unit()).apply(new Cons<>(resume)));
    }
  }

  static final class Up<R> extends Process<R, Cons_, Send> implements Send {

    Up(Kind<Cons_, Control<R>> init) {
      super(init);
    }

    @Override
    public Send effect() { return this; }

    @Override
    public Control<Unit> send(int n) {
      return useState(state -> resume -> state.fix(toCons()).apply(n).apply(new Prod<>(resume)));
    }
  }
}

@HigherKind
final class Prod<R> implements ProdOf<R> {

  private final Function1<Unit, Function1<Kind<Cons_, R>, R>> apply;

  Prod(Function1<Unit, Function1<Kind<Cons_, R>, R>> apply) {
    this.apply = requireNonNull(apply);
  }

  public Function1<Kind<Cons_, R>, R> apply(Unit unit) {
    return apply.apply(unit);
  }
}

@HigherKind
final class Cons<R> implements ConsOf<R> {

  private final Function1<Integer, Function1<Kind<Prod_, R>, R>> apply;

  Cons(Function1<Integer, Function1<Kind<Prod_, R>, R>> apply) {
    this.apply = requireNonNull(apply);
  }

  public Function1<Kind<Prod_, R>, R> apply(int n) {
    return apply.apply(n);
  }
}
