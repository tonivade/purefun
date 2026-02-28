/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.core.Unit.unit;
import java.util.Arrays;
import java.util.stream.Stream;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Instances;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public sealed interface PureStream<F extends Kind<F, ?>, T>
  extends PureStreamOf<F, T>, Bindable<PureStream<F, ?>, T>
    permits Cons, Suspend, Nil {

  default PureStream<F, T> head() {
    return take(1);
  }

  default PureStream<F, T> tail() {
    return drop(1);
  }

  Kind<F, Option<T>> headOption();
  Kind<F, Option<Tuple2<Kind<F, T>, PureStream<F, T>>>> split();

  PureStream<F, T> concat(PureStream<F, ? extends T> other);
  PureStream<F, T> append(Kind<F, ? extends T> other);
  PureStream<F, T> prepend(Kind<F, ? extends T> other);

  PureStream<F, T> take(int n);
  PureStream<F, T> drop(int n);

  PureStream<F, T> filter(Matcher1<? super T> matcher);
  PureStream<F, T> takeWhile(Matcher1<? super T> matcher);
  PureStream<F, T> dropWhile(Matcher1<? super T> matcher);

  default PureStream<F, T> filterNot(Matcher1<? super T> matcher) {
    return filter(matcher.negate());
  }

  <R> PureStream<F, R> collect(PartialFunction1<? super T, ? extends R> partial);
  <R> Kind<F, R> foldLeft(R begin, Function2<? super R, ? super T, ? extends R> combinator);
  <R> Kind<F, R> foldRight(Kind<F, ? extends R> begin,
      Function2<? super T, ? super Kind<F, ? extends R>, ? extends Kind<F, ? extends R>> combinator);

  @Override
  <R> PureStream<F, R> map(Function1<? super T, ? extends R> map);
  @Override
  <R> PureStream<F, R> flatMap(Function1<? super T, ? extends Kind<PureStream<F, ?>, ? extends R>> map);
  @Override
  default <R> PureStream<F, R> andThen(Kind<PureStream<F, ?>, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  <R> PureStream<F, R> mapEval(Function1<? super T, ? extends Kind<F, ? extends R>> mapper);

  PureStream<F, T> repeat();
  PureStream<F, T> intersperse(Kind<F, ? extends T> value);

  Kind<F, Boolean> exists(Matcher1<? super T> matcher);
  Kind<F, Boolean> forall(Matcher1<? super T> matcher);

  default <G extends Kind<G, ?>, R> PureStream<G, R> through(Function1<PureStream<F, T>, PureStream<G, R>> function) {
    return function.apply(this);
  }

  default Kind<F, Sequence<T>> asSequence() {
    return foldLeft(ImmutableList.empty(), Sequence::append);
  }

  default Kind<F, String> asString() {
    return foldLeft("", (acc, a) -> acc + a);
  }

  default Kind<F, Unit> drain() {
    return foldLeft(unit(), (acc, a) -> acc);
  }

  default <R> PureStream<F, R> mapReplace(Kind<F, ? extends R> next) {
    return mapEval(ignore -> next);
  }

  static <F extends Kind<F, ?>> PureStream.Of<F> of(MonadDefer<F> monad) {
    return () -> monad;
  }

  static <F extends Kind<F, ?>> PureStream.Of<F> of(Class<F> type) {
    return of(Instances.monadDefer(type));
  }

  @SafeVarargs
  static <F extends Kind<F, ?>> PureStream.Of<F> of(F...reified) {
    return of(Instances.monadDefer(reified));
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> empty(F... reified) {
    return of(Instances.monadDefer(reified)).empty();
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> pure(T value, F...reified) {
    return of(Instances.monadDefer(reified)).pure(value);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> cons(T head, PureStream<F, ? extends T> tail, F...reified) {
    return of(Instances.monadDefer(reified)).cons(head, tail);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> suspend(Producer<? extends PureStream<F, ? extends T>> lazy, F...reified) {
    return of(Instances.monadDefer(reified)).suspend(lazy);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> eval(Kind<F, ? extends T> value, F...reified) {
    return of(Instances.monadDefer(reified)).eval(value);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> from(Iterable<? extends T> iterable, F...reified) {
    return of(Instances.monadDefer(reified)).from(iterable);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> from(Stream<? extends T> stream, F...reified) {
    return of(Instances.monadDefer(reified)).from(stream);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> from(Sequence<? extends T> sequence, F...reified) {
    return of(Instances.monadDefer(reified)).from(sequence);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T, S> PureStream<F, T> unfold(
      S seed, Function1<? super S, Option<Tuple2<? extends T, ? extends S>>> function, F...reified) {
    return of(Instances.monadDefer(reified)).unfold(seed, function);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> iterate(T seed, Operator1<T> generator, F...reified) {
    return of(Instances.monadDefer(reified)).iterate(seed, generator);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, T> PureStream<F, T> iterate(Producer<? extends T> generator, F...reified) {
    return of(Instances.monadDefer(reified)).iterate(generator);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, A, B, R> PureStream<F, R> zipWith(PureStream<F, ? extends A> s1, PureStream<F, ? extends B> s2,
      Function2<? super A, ? super B, ? extends R> combinator, F...reified) {
    return of(Instances.monadDefer(reified)).zipWith(s1, s2, combinator);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, A, B> PureStream<F, Tuple2<A, B>> zip(PureStream<F, ? extends A> s1, PureStream<F, ? extends B> s2, F...reified) {
    return of(Instances.monadDefer(reified)).zip(s1, s2);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, A> PureStream<F, Tuple2<A, Integer>> zipWithIndex(PureStream<F, ? extends A> stream, F...reified) {
    return of(Instances.monadDefer(reified)).zipWithIndex(stream);
  }

  @SafeVarargs
  static <F extends Kind<F, ?>, A> PureStream<F, A> merge(PureStream<F, A> s1, PureStream<F, A> s2, F...reified) {
    return of(Instances.monadDefer(reified)).merge(s1, s2);
  }

  interface Of<F extends Kind<F, ?>> {

    MonadDefer<F> monadDefer();

    default <T> PureStream<F, T> empty() {
      return new Nil<>(monadDefer());
    }

    @SuppressWarnings("unchecked")
    default <T> PureStream<F, T> of(T... values) {
      return from(Arrays.stream(values));
    }

    default <T> PureStream<F, T> pure(T value) {
      return eval(monadDefer().pure(value));
    }

    default <T> PureStream<F, T> cons(T head, PureStream<F, ? extends T> tail) {
      return pure(head).concat(tail);
    }

    default <T> PureStream<F, T> suspend(Producer<? extends PureStream<F, ? extends T>> lazy) {
      return new Suspend<>(monadDefer(),
          monadDefer().defer(
              lazy.andThen(PureStreamOf::<F, T>toPureStream).map(monadDefer()::<PureStream<F, T>>pure)));
    }

    default <T> PureStream<F, T> eval(Kind<F, ? extends T> value) {
      return new Cons<>(monadDefer(), Kind.narrowK(value), empty());
    }

    default <T> PureStream<F, T> from(Iterable<? extends T> iterable) {
      return from(ImmutableList.from(iterable));
    }

    default <T> PureStream<F, T> from(java.util.stream.Stream<? extends T> stream) {
      return from(ImmutableList.from(stream::iterator));
    }

    default <T> PureStream<F, T> from(Sequence<? extends T> sequence) {
      return sequence.foldLeft(empty(), (acc, a) -> acc.append(monadDefer().pure(a)));
    }

    default <T, S> PureStream<F, T> unfold(S seed, Function1<? super S, Option<Tuple2<? extends T, ? extends S>>> function) {
      return suspend(() -> doUnfold(seed, function));
    }

    default <T> PureStream<F, T> iterate(T seed, Operator1<T> generator) {
      return cons(seed, suspend(() -> iterate(generator.apply(seed), generator)));
    }

    default <T> PureStream<F, T> iterate(Producer<? extends T> generator) {
      return unfold(unit(), unit -> Option.of(generator).map(next -> Tuple.of(next, unit)));
    }

    default <A, B, R> PureStream<F, R> zipWith(PureStream<F, ? extends A> s1, PureStream<F, ? extends B> s2,
        Function2<? super A, ? super B, ? extends R> combinator) {
      return new Suspend<>(monadDefer(), monadDefer().defer(
        () -> monadDefer().mapN(s1.split(), s2.split()).apply(
          (op1, op2) -> {
            Option<PureStream<F, R>> result = Option.map2(op1, op2,
              (t1, t2) -> {
                Kind<F, R> head = monadDefer().mapN(t1.get1(), t2.get1()).apply(combinator);
                PureStream<F, R> tail = zipWith(t1.get2(), t2.get2(), combinator);
                return new Cons<>(monadDefer(), head, tail);
              });
            return result.getOrElse(this::empty);
          })
        ));
    }

    default <A, B> PureStream<F, Tuple2<A, B>> zip(PureStream<F, ? extends A> s1, PureStream<F, ? extends B> s2) {
      return zipWith(s1, s2, Tuple2::of);
    }

    default <A> PureStream<F, Tuple2<A, Integer>> zipWithIndex(PureStream<F, ? extends A> stream) {
      return zip(stream, iterate(0, x -> x + 1));
    }

    // TODO: generics
    default <A> PureStream<F, A> merge(PureStream<F, A> s1, PureStream<F, A> s2) {
      return new Suspend<>(monadDefer(), monadDefer().defer(
        () -> monadDefer().mapN(s1.split(), s2.split()).apply(
          (opt1, opt2) -> {
            Option<PureStream<F, A>> result = Option.map2(opt1, opt2,
              (t1, t2) -> {
                Kind<F, A> head = t1.get1();
                PureStream<F, A> tail = eval(t2.get1()).concat(merge(t1.get2(), t2.get2()));
                return new Cons<>(monadDefer(), head, tail);
              });
            return result.getOrElse(this::empty);
          })
        ));
    }

    private <T, S> PureStream<F, T> doUnfold(S seed, Function1<? super S, Option<Tuple2<? extends T, ? extends S>>> function) {
      return function.apply(seed)
        .map(tuple -> tuple.applyTo((t, s) -> cons(t, suspend(() -> doUnfold(s, function)))))
        .getOrElse(this::empty);
    }
  }
}