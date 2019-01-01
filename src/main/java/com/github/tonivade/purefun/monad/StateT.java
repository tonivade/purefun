/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static java.util.Objects.requireNonNull;

import com.github.tonivade.purefun.FlatMap3;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Higher3;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.Transformer;

public final class StateT<F extends Kind, S, A> implements FlatMap3<StateT.µ, F, S, A> {

  public static final class µ implements Kind {}

  private final Monad<F> monad;
  private final Function1<S, Higher1<F, Tuple2<S, A>>> run;

  private StateT(Monad<F> monad, Function1<S, Higher1<F, Tuple2<S, A>>> run) {
    this.monad = requireNonNull(monad);
    this.run = requireNonNull(run);
  }

  public Higher1<F, Tuple2<S, A>> run(S state) {
    return run.apply(state);
  }

  public Higher1<F, A> eval(S state) {
    return monad.map(run(state), Tuple2::get2);
  }

  @Override
  public <R> StateT<F, S, R> map(Function1<A, R> map) {
    return flatMap(value -> pure(monad, map.apply(value)));
  }

  @Override
  public <R> StateT<F, S, R> flatMap(Function1<A, ? extends Higher3<StateT.µ, F, S, R>> map) {
    return state(monad, state -> {
      Higher1<F, Tuple2<S, A>> newState = run(state);
      return monad.flatMap(newState, state2 -> map.andThen(StateT::narrowK).apply(state2.get2()).run(state2.get1()));
    });
  }

  public <G extends Kind> StateT<G, S, A> mapK(Monad<G> other, Transformer<F, G> transformer) {
    return new StateT<>(other, state -> transformer.apply(run(state)));
  }

  public static <F extends Kind, S, A> StateT<F, S, A> state(Monad<F> monad, Function1<S, Higher1<F, Tuple2<S, A>>> run) {
    return new StateT<>(monad, run);
  }

  public static <F extends Kind, S, A> StateT<F, S, A> lift(Monad<F> monad, Function1<S, Tuple2<S, A>> run) {
    return state(monad, run.andThen(monad::pure));
  }

  public static <F extends Kind, S, A> StateT<F, S, A> pure(Monad<F> monad, A value) {
    return lift(monad, state -> Tuple2.of(state, value));
  }

  public static <F extends Kind, S> StateT<F, S, S> get(Monad<F> monad) {
    return lift(monad, state -> Tuple2.of(state, state));
  }

  public static <F extends Kind, S> StateT<F, S, Nothing> set(Monad<F> monad, S value) {
    return lift(monad, state -> Tuple2.of(value, nothing()));
  }

  public static <F extends Kind, S> StateT<F, S, Nothing> modify(Monad<F> monad, Operator1<S> mapper) {
    return lift(monad, state -> Tuple2.of(mapper.apply(state), nothing()));
  }

  public static <F extends Kind, S, A> StateT<F, S, A> inspect(Monad<F> monad, Function1<S, A> mapper) {
    return lift(monad, state -> Tuple2.of(state, mapper.apply(state)));
  }

  public static <F extends Kind, S, A> StateT<F, S, Sequence<A>> compose(Monad<F> monad,
                                                                         Sequence<StateT<F, S, A>> states) {
    return states.foldLeft(pure(monad, ImmutableList.empty()), (sa, sb) -> map2(sa, sb, (acc, a) -> acc.append(a)));
  }

  public static <F extends Kind, S, A, B, C> StateT<F, S, C> map2(StateT<F, S, A> sa,
                                                                  StateT<F, S, B> sb,
                                                                  Function2<A, B, C> mapper) {
    return sa.flatMap(a -> sb.map(b -> mapper.curried().apply(a).apply(b)));
  }

  public static <F extends Kind, S, A> StateT<F, S, A> of(Monad<F> monad, Function1<S, Higher1<F, Tuple2<S, A>>> run) {
    return state(monad, run);
  }

  public static <F extends Kind, S> Monad<Higher1<Higher1<StateT.µ, F>, S>> monad(Monad<F> monadF) {
    return new Monad<Higher1<Higher1<StateT.µ, F>, S>>() {

      @Override
      public <T> StateT<F, S, T> pure(T value) {
        return StateT.pure(monadF, value);
      }

      @Override
      public <T, R> StateT<F, S, R> flatMap(Higher1<Higher1<Higher1<StateT.µ, F>, S>, T> value,
          Function1<T, ? extends Higher1<Higher1<Higher1<StateT.µ, F>, S>, R>> map) {
        return StateT.narrowK(value).flatMap(map.andThen(StateT::narrowK));
      }
    };
  }

  public static <F extends Kind, S, A> StateT<F, S, A> narrowK(Higher3<StateT.µ, F, S, A> hkt) {
    return (StateT<F, S, A>) hkt;
  }

  public static <F extends Kind, S, A> StateT<F, S, A> narrowK(Higher2<Higher1<StateT.µ, F>, S, A> hkt) {
    return (StateT<F, S, A>) hkt;
  }

  @SuppressWarnings("unchecked")
  public static <F extends Kind, S, A> StateT<F, S, A> narrowK(Higher1<Higher1<Higher1<StateT.µ, F>, S>, A> hkt) {
    // XXX: I don't know why, but compiler says here there's an unsafe cast
    return (StateT<F, S, A>) hkt;
  }
}
