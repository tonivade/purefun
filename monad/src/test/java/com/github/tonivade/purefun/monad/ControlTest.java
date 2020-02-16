/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.ControlInstances;
import com.github.tonivade.purefun.typeclasses.For;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.monad.Control.pure;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ControlTest {

  interface Amb {
    Control<Boolean> flip();
  }

  interface State<S> {
    Control<S> get();
    Control<Unit> set(S state);
  }

  static class SimpleTest {

    @Test
    public void test() {
      Control<ImmutableList<Integer>> handled = ambList(this::program);

      assertEquals(listOf(2, 3), handled.run());
    }

    private Control<Integer> program(Amb amb) {
      return amb.flip().map(x -> x ? 2 : 3);
    }
  }

  static class StatefulTest {

    @Test
    public void test1() {
      Control<ImmutableList<Integer>> handled = ambList(amb -> state(0, state -> program(state, amb)));

      assertEquals(listOf(1, 0), handled.run());
    }

    @Test
    public void test2() {
      Control<ImmutableList<Integer>> handled = state(0, state -> ambList(amb -> program(state, amb)));

      assertEquals(listOf(1, 1), handled.run());
    }

    private Control<Integer> program(State<Integer> state, Amb amb) {
      return For.with(ControlInstances.monad())
          .and(state.get().kind1())
          .flatMap(x -> amb.flip().flatMap(b -> b ? state.set(x + 1) : pure(unit())).kind1())
          .and(state.get().kind1())
          .fix(Control::narrowK);
    }
  }

  private static <R> Control<ImmutableList<R>> ambList(Function1<Amb, Control<R>> program) {
    return new AmbList<R>().<AmbList<R>>apply(amb -> program.apply(amb).map(ImmutableList::of));
  }

  private static final class AmbList<R> implements Control.Handler<ImmutableList<R>>, Amb {

    @Override
    public Control<Boolean> flip() {
      return use(resume ->
          resume.apply(true).flatMap(ts -> resume.apply(false).map(ts::appendAll)));
    }
  }

  static <R, S> Control<R> state(S init, Function1<State<S>, Control<R>> program) {
    return new StateImpl<R, S>().<StateImpl<R, S>>apply(
        state -> program.apply(state).map(r -> s -> pure(r))).flatMap(f -> f.apply(init));
  }

  static final class StateImpl<R, S> implements Control.Handler<Function1<S, Control<R>>>, State<S> {

    @Override
    public Control<S> get() {
      return use(resume -> Control.later(() -> s -> resume.apply(s).flatMap(f -> f.apply(s))));
    }

    @Override
    public Control<Unit> set(S state) {
      return use(resume -> Control.later(() -> s -> resume.apply(unit()).flatMap(f -> f.apply(state))));
    }
  }
}