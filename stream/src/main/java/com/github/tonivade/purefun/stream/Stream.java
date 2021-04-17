/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Unit.unit;
import static com.github.tonivade.purefun.data.Sequence.asStream;

import java.util.Arrays;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Bindable;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind(sealed = true)
public interface Stream<F extends Witness, T> extends StreamOf<F, T>, Bindable<Kind<Stream_, F>, T> {

  default Stream<F, T> head() {
    return take(1);
  }

  default Stream<F, T> tail() {
    return drop(1);
  }

  Kind<F, Option<T>> headOption();
  Kind<F, Option<Tuple2<Kind<F, T>, Stream<F, T>>>> split();

  Stream<F, T> concat(Stream<F, ? extends T> other);
  Stream<F, T> append(Kind<F, ? extends T> other);
  Stream<F, T> prepend(Kind<F, ? extends T> other);

  Stream<F, T> take(int n);
  Stream<F, T> drop(int n);

  Stream<F, T> filter(Matcher1<? super T> matcher);
  Stream<F, T> takeWhile(Matcher1<? super T> matcher);
  Stream<F, T> dropWhile(Matcher1<? super T> matcher);

  default Stream<F, T> filterNot(Matcher1<? super T> matcher) {
    return filter(matcher.negate());
  }

  <R> Stream<F, R> collect(PartialFunction1<? super T, ? extends R> partial);
  <R> Kind<F, R> foldLeft(R begin, Function2<? super R, ? super T, ? extends R> combinator);
  <R> Kind<F, R> foldRight(Kind<F, ? extends R> begin, 
      Function2<? super T, ? super Kind<F, ? extends R>, ? extends Kind<F, ? extends R>> combinator);

  @Override
  <R> Stream<F, R> map(Function1<? super T, ? extends R> map);
  @Override
  <R> Stream<F, R> flatMap(Function1<? super T, ? extends Kind<Kind<Stream_, F>, ? extends R>> map);
  @Override
  default <R> Stream<F, R> andThen(Kind<Kind<Stream_, F>, ? extends R> next) {
    return flatMap(ignore -> next);
  }

  <R> Stream<F, R> mapEval(Function1<? super T, ? extends Kind<F, ? extends R>> mapper);

  Stream<F, T> repeat();
  Stream<F, T> intersperse(Kind<F, ? extends T> value);

  Kind<F, Boolean> exists(Matcher1<? super T> matcher);
  Kind<F, Boolean> forall(Matcher1<? super T> matcher);

  default <G extends Witness, R> Stream<G, R> through(Function1<Stream<F, T>, Stream<G, R>> function) {
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

  default <R> Stream<F, R> mapReplace(Kind<F, ? extends R> next) {
    return mapEval(ignore -> next);
  }

  static <F extends Witness> StreamOf<F> of(MonadDefer<F> monad) {
    return () -> monad;
  }

  interface StreamOf<F extends Witness> {

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

    default <T> Stream<F, T> cons(T head, Stream<F, ? extends T> tail) {
      return pure(head).concat(tail);
    }

    default <T> Stream<F, T> suspend(Producer<? extends Stream<F, ? extends T>> lazy) {
      return new Suspend<>(monadDefer(), 
          monadDefer().defer(
              lazy.andThen(com.github.tonivade.purefun.stream.StreamOf::<F, T>narrowK).map(monadDefer()::<Stream<F, T>>pure)));
    }

    default <T> Stream<F, T> eval(Kind<F, ? extends T> value) {
      return new Cons<>(monadDefer(), Kind.narrowK(value), empty());
    }

    default <T> Stream<F, T> from(Iterable<? extends T> iterable) {
      return from(asStream(iterable.iterator()));
    }

    default <T> Stream<F, T> from(java.util.stream.Stream<? extends T> stream) {
      return from(ImmutableList.from(stream));
    }

    default <T> Stream<F, T> from(Sequence<? extends T> sequence) {
      return sequence.foldLeft(empty(), (acc, a) -> acc.append(monadDefer().pure(a)));
    }

    default <T, S> Stream<F, T> unfold(S seed, Function1<? super S, Option<Tuple2<? extends T, S>>> function) {
      return suspend(() -> StreamModule.unfold(this, seed, function));
    }

    default <T> Stream<F, T> iterate(T seed, Operator1<T> generator) {
      return cons(seed, suspend(() -> iterate(generator.apply(seed), generator)));
    }

    default <T> Stream<F, T> iterate(Producer<? extends T> generator) {
      return unfold(unit(), unit -> Option.of(generator).map(next -> Tuple.of(next, unit)));
    }

    default <A, B, R> Stream<F, R> zipWith(Stream<F, ? extends A> s1, Stream<F, ? extends B> s2, 
        Function2<? super A, ? super B, ? extends R> combinator) {
      return new Suspend<>(monadDefer(), monadDefer().defer(
        () -> monadDefer().mapN(s1.split(), s2.split(),
          (op1, op2) -> {
            Option<Stream<F, R>> result = Option.map2(op1, op2,
              (t1, t2) -> {
                Kind<F, R> head = monadDefer().mapN(t1.get1(), t2.get1(), combinator);
                Stream<F, R> tail = zipWith(t1.get2(), t2.get2(), combinator);
                return new Cons<>(monadDefer(), head, tail);
              });
            return result.getOrElse(this::empty);
          })
        ));
    }

    default <A, B> Stream<F, Tuple2<A, B>> zip(Stream<F, ? extends A> s1, Stream<F, ? extends B> s2) {
      return zipWith(s1, s2, Tuple2::of);
    }

    default <A> Stream<F, Tuple2<A, Integer>> zipWithIndex(Stream<F, ? extends A> stream) {
      return zip(stream, iterate(0, x -> x + 1));
    }

    // TODO: generics
    default <A> Stream<F, A> merge(Stream<F, A> s1, Stream<F, A> s2) {
      return new Suspend<>(monadDefer(), monadDefer().defer(
        () -> monadDefer().mapN(s1.split(), s2.split(),
          (opt1, opt2) -> {
            Option<Stream<F, A>> result = Option.map2(opt1, opt2,
              (t1, t2) -> {
                Kind<F, A> head = t1.get1();
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

  static <F extends Witness, T, S> Stream<F, T> unfold(Stream.StreamOf<F> streamOf, S seed,
                                                       Function1<? super S, Option<Tuple2<? extends T, S>>> function) {
    return function.apply(seed)
      .map(tuple -> streamOf.cons(tuple.get1(), streamOf.<T>suspend(() -> unfold(streamOf, tuple.get2(), function))))
      .getOrElse(streamOf::empty);
  }
}