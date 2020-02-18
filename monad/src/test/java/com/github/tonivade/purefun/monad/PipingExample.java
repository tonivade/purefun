/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Unit;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

public class PipingExample {

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

  private <R> Control<R> pipe(Function1<Down<R>, Control<R>> down, Function1<Up<R>, Control<R>> up) {
    return down(new Prod<Control<R>>(unit -> cons -> up(cons.fix1(Cons::narrowK)).apply(up))).apply(down);
  }

  @Test
  public void program() {
    pipe(this::consumer, this::producer).run();
  }

  private <R> Down<R> down(Prod<Control<R>> p) {
    return new Down<>(p.kind1());
  }

  private <R> Up<R> up(Cons<Control<R>> p) {
    return new Up<>(p.kind1());
  }

  private Control<Unit> println(Integer x) {
    return Control.later(() -> { System.out.println(x); return unit(); });
  }

  static class Process<R, P extends Kind> extends Control.Stateful<R, Higher1<P, Control<R>>> {

    Process(Higher1<P, Control<R>> init) {
      super(init);
    }
  }

  static final class Down<R> extends Process<R, Prod.µ> implements Receive {

    Down(Higher1<Prod.µ, Control<R>> init) {
      super(init);
    }

    @Override
    public Control<Integer> receive() {
      return useState(state -> resume -> state.fix1(Prod::narrowK).apply(unit()).apply(new Cons<>(resume).kind1()));
    }
  }

  static final class Up<R> extends Process<R, Cons.µ> implements Send {

    Up(Higher1<Cons.µ, Control<R>> init) {
      super(init);
    }

    @Override
    public Control<Unit> send(int n) {
      return useState(state -> resume -> state.fix1(Cons::narrowK).apply(n).apply(new Prod<>(resume).kind1()));
    }
  }
}

@HigherKind
final class Prod<R> {

  private final Function1<Unit, Function1<Higher1<Cons.µ, R>, R>> apply;

  Prod(Function1<Unit, Function1<Higher1<Cons.µ, R>, R>> apply) {
    this.apply = requireNonNull(apply);
  }

  public Function1<Higher1<Cons.µ, R>, R> apply(Unit unit) {
    return apply.apply(unit);
  }
}

@HigherKind
final class Cons<R> {

  private final Function1<Integer, Function1<Higher1<Prod.µ, R>, R>> apply;

  Cons(Function1<Integer, Function1<Higher1<Prod.µ, R>, R>> apply) {
    this.apply = requireNonNull(apply);
  }

  public Function1<Higher1<Prod.µ, R>, R> apply(int n) {
    return apply.apply(n);
  }
}
