/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.asStream;

import java.util.Arrays;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Sealed;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@Sealed
@HigherKind
public interface Stream<F extends Kind, T> {

  default Stream<F, T> head() {
    return take(1);
  }

  default Stream<F, T> tail() {
    return drop(1);
  }

  Higher1<F, Option<T>> headOption();
  Higher1<F, Option<Tuple2<Higher1<F, T>, Stream<F, T>>>> split();

  Stream<F, T> concat(Stream<F, T> other);
  Stream<F, T> append(Higher1<F, T> other);
  Stream<F, T> prepend(Higher1<F, T> other);

  Stream<F, T> take(int n);
  Stream<F, T> drop(int n);

  Stream<F, T> filter(Matcher1<T> matcher);
  Stream<F, T> takeWhile(Matcher1<T> matcher);
  Stream<F, T> dropWhile(Matcher1<T> matcher);

  default Stream<F, T> filterNot(Matcher1<T> matcher) {
    return filter(matcher.negate());
  }

  <R> Stream<F, R> collect(PartialFunction1<T, R> partial);
  <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator);
  <R> Higher1<F, R> foldRight(Higher1<F, R> begin, Function2<T, Higher1<F, R>, Higher1<F, R>> combinator);

  <R> Stream<F, R> map(Function1<T, R> map);
  <R> Stream<F, R> flatMap(Function1<T, Stream<F, R>> map);
  <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper);

  Stream<F, T> repeat();
  Stream<F, T> intersperse(Higher1<F, T> value);

  Higher1<F, Boolean> exists(Matcher1<T> matcher);
  Higher1<F, Boolean> forall(Matcher1<T> matcher);

  default <G extends Kind, R> Stream<G, R> through(Function1<Stream<F, T>, Stream<G, R>> function) {
    return function.apply(this);
  }

  default Higher1<F, Sequence<T>> asSequence() {
    return foldLeft(ImmutableList.empty(), (acc, a) -> acc.append(a));
  }

  default Higher1<F, String> asString() {
    return foldLeft("", (acc, a) -> acc + a);
  }

  default Higher1<F, Unit> drain() {
    return foldLeft(unit(), (acc, a) -> acc);
  }

  default <R> Stream<F, R> andThen(Higher1<F, R> next) {
    return mapEval(ignore -> next);
  }

  StreamModule getModule();

  static <F extends Kind> StreamOf<F> of(MonadDefer<F> monad) {
    return () -> monad;
  }

  interface StreamOf<F extends Kind> {

    MonadDefer<F> monadDefer();

    default <T> Stream<F, T> empty() {
      return new Nil<>(monadDefer());
    }

    @SuppressWarnings("unchecked")
    default <T> Stream<F, T> of(T... values) {
      return from(Arrays.stream(values));
    }

    default <T> Stream<F, T> pure(T value) {
      return eval(monadDefer().pure(value));
    }

    default <T> Stream<F, T> cons(T head, Stream<F, T> tail) {
      return pure(head).concat(tail);
    }

    default <T> Stream<F, T> suspend(Producer<Stream<F, T>> lazy) {
      return new Suspend<>(monadDefer(), monadDefer().defer(lazy.map(monadDefer()::<Stream<F, T>>pure)));
    }

    default <T> Stream<F, T> eval(Higher1<F, T> value) {
      return new Cons<>(monadDefer(), value, empty());
    }

    default <T> Stream<F, T> from(Iterable<T> iterable) {
      return from(asStream(iterable.iterator()));
    }

    default <T> Stream<F, T> from(java.util.stream.Stream<T> stream) {
      return from(ImmutableList.from(stream));
    }

    default <T> Stream<F, T> from(Sequence<T> sequence) {
      return sequence.foldLeft(empty(), (acc, a) -> acc.append(monadDefer().pure(a)));
    }

    default <T, S> Stream<F, T> unfold(S seed, Function1<S, Option<Tuple2<T, S>>> function) {
      return suspend(() -> StreamModule.unfold(this, seed, function));
    }

    default <T> Stream<F, T> iterate(T seed, Operator1<T> generator) {
      return cons(seed, suspend(() -> iterate(generator.apply(seed), generator)));
    }

    default <T> Stream<F, T> iterate(Producer<T> generator) {
      return unfold(unit(), unit -> Option.of(generator).map(next -> Tuple.of(next, unit)));
    }

    default <A, B, R> Stream<F, R> zipWith(Stream<F, A> s1, Stream<F, B> s2, Function2<A, B, R> combinator) {
      return new Suspend<>(monadDefer(), monadDefer().defer(
        () -> monadDefer().map2(s1.split(), s2.split(),
          (op1, op2) -> {
            Option<Stream<F, R>> result = StreamModule.map2(op1, op2,
              (t1, t2) -> {
                Higher1<F, R> head = monadDefer().map2(t1.get1(), t2.get1(), combinator);
                Stream<F, R> tail = zipWith(t1.get2(), t2.get2(), combinator);
                return new Cons<>(monadDefer(), head, tail);
              });
            return result.getOrElse(this::empty);
          })
        ));
    }

    default <A, B> Stream<F, Tuple2<A, B>> zip(Stream<F, A> s1, Stream<F, B> s2) {
      return zipWith(s1, s2, Tuple2::of);
    }

    default <A> Stream<F, Tuple2<A, Integer>> zipWithIndex(Stream<F, A> stream) {
      return zip(stream, iterate(0, x -> x + 1));
    }

    default <A> Stream<F, A> merge(Stream<F, A> s1, Stream<F, A> s2) {
      return new Suspend<>(monadDefer(), monadDefer().defer(
        () -> monadDefer().map2(s1.split(), s2.split(),
          (opt1, opt2) -> {
            Option<Stream<F, A>> result = StreamModule.map2(opt1, opt2,
              (t1, t2) -> {
                Higher1<F, A> head = t1.get1();
                Stream<F, A> tail = eval(t2.get1()).concat(merge(t1.get2(), t2.get2()));
                return new Cons<>(monadDefer(), head, tail);
              });
            return result.getOrElse(this::empty);
          })
        ));
    }
  }
}

interface StreamModule {
  static <A, B, C> Option<C> map2(Option<A> fa, Option<B> fb, Function2<A, B, C> combiner) {
    return fa.flatMap(a -> fb.map(b -> combiner.apply(a, b)));
  }

  static <F extends Kind, T, S> Stream<F, T> unfold(Stream.StreamOf<F> streamOf, S seed,
                                                    Function1<S, Option<Tuple2<T, S>>> function) {
    return function.apply(seed)
      .map(tuple -> streamOf.cons(tuple.get1(), streamOf.suspend(() -> unfold(streamOf, tuple.get2(), function))))
      .getOrElse(streamOf::empty);
  }
}