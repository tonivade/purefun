/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.transformer;

import static com.github.tonivade.purefun.Unit.unit;

import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Transformer;

@HigherKind
public interface StateT<F extends Kind, S, A> extends FlatMap3<StateT.µ, F, S, A> {

  Monad<F> monad();
  Higher1<F, Tuple2<S, A>> run(S state);

  default Higher1<F, A> eval(S state) {
    return monad().map(run(state), Tuple2::get2);
  }

  @Override
  default <R> StateT<F, S, R> map(Function1<A, R> map) {
    return flatMap(value -> pure(monad(), map.apply(value)));
  }

  @Override
  default <R> StateT<F, S, R> flatMap(Function1<A, ? extends Higher3<StateT.µ, F, S, R>> map) {
    return state(monad(), state -> {
      Higher1<F, Tuple2<S, A>> newState = run(state);
      return monad().flatMap(newState, state2 -> map.andThen(StateT::narrowK).apply(state2.get2()).run(state2.get1()));
    });
  }

  default <G extends Kind> StateT<G, S, A> mapK(Monad<G> other, Transformer<F, G> transformer) {
    return state(other, state -> transformer.apply(run(state)));
  }

  static <F extends Kind, S, A> StateT<F, S, A> state(Monad<F> monad, Function1<S, Higher1<F, Tuple2<S, A>>> run) {
    return new StateT<F, S, A>() {

      @Override
      public Monad<F> monad() { return monad; }

      @Override
      public Higher1<F, Tuple2<S, A>> run(S state) { return run.apply(state); }
    };
  }

  static <F extends Kind, S, A> StateT<F, S, A> lift(Monad<F> monad, Function1<S, Tuple2<S, A>> run) {
    return state(monad, run.andThen(monad::pure));
  }

  static <F extends Kind, S, A> StateT<F, S, A> pure(Monad<F> monad, A value) {
    return lift(monad, state -> Tuple2.of(state, value));
  }

  static <F extends Kind, S> StateT<F, S, S> get(Monad<F> monad) {
    return lift(monad, state -> Tuple2.of(state, state));
  }

  static <F extends Kind, S> StateT<F, S, Unit> set(Monad<F> monad, S value) {
    return lift(monad, state -> Tuple2.of(value, unit()));
  }

  static <F extends Kind, S> StateT<F, S, Unit> modify(Monad<F> monad, Operator1<S> mapper) {
    return lift(monad, state -> Tuple2.of(mapper.apply(state), unit()));
  }

  static <F extends Kind, S, A> StateT<F, S, A> inspect(Monad<F> monad, Function1<S, A> mapper) {
    return lift(monad, state -> Tuple2.of(state, mapper.apply(state)));
  }

  static <F extends Kind, S, A> StateT<F, S, Sequence<A>> compose(Monad<F> monad,
                                                                         Sequence<StateT<F, S, A>> states) {
    return states.foldLeft(pure(monad, ImmutableList.empty()), (sa, sb) -> map2(sa, sb, (acc, a) -> acc.append(a)));
  }

  static <F extends Kind, S, A, B, C> StateT<F, S, C> map2(StateT<F, S, A> sa,
                                                                  StateT<F, S, B> sb,
                                                                  Function2<A, B, C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  static <F extends Kind, S, A> StateT<F, S, A> of(Monad<F> monad, Function1<S, Higher1<F, Tuple2<S, A>>> run) {
    return state(monad, run);
  }
}
