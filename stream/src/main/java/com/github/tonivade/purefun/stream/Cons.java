/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

import static com.github.tonivade.purefun.Precondition.checkNonNull;

public final class Cons<F extends Witness, T> implements Stream<F, T> {

  private final MonadDefer<F> monad;
  private final Kind<F, T> head;
  private final Stream<F, T> tail;

  Cons(MonadDefer<F> monad, Kind<F, T> head, Stream<F, T> tail) {
    this.monad = checkNonNull(monad);
    this.head = checkNonNull(head);
    this.tail = checkNonNull(tail);
  }

  @Override
  public Kind<F, Option<T>> headOption() {
    return monad.map(head, Option::some);
  }

  @Override
  public Kind<F, Option<Tuple2<Kind<F, T>, Stream<F, T>>>> split() {
    return monad.pure(Option.some(Tuple.of(head, tail)));
  }

  @Override
  public Stream<F, T> concat(Stream<F, ? extends T> other) {
    return suspend(() -> cons(head, tail.concat(other)));
  }

  @Override
  public Stream<F, T> append(Kind<F, ? extends T> other) {
    return suspend(() -> cons(head, tail.append(other)));
  }

  @Override
  public Stream<F, T> prepend(Kind<F, ? extends T> other) {
    return suspend(() -> cons(Kind.narrowK(other), tail.prepend(head)));
  }

  @Override
  public Stream<F, T> take(int n) {
    return n > 0 ? suspend(() -> cons(head, tail.take(n - 1))) : empty();
  }

  @Override
  public Stream<F, T> drop(int n) {
    return n > 0 ? suspend(() -> tail.drop(n - 1)) : this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<? super T> matcher) {
    return suspendF(() -> monad.map(head,
        t -> matcher.match(t) ? cons(head, tail.takeWhile(matcher)) : empty()));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<? super T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                tail.dropWhile(matcher) : this));
  }

  @Override
  public Stream<F, T> filter(Matcher1<? super T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                cons(head, tail.filter(matcher)) : tail.filter(matcher)));
  }

  @Override
  public <R> Stream<F, R> collect(PartialFunction1<? super T, ? extends R> partial) {
    return suspendF(() ->
            monad.map(head, t -> partial.isDefinedAt(t) ?
                cons(monad.map(head, partial::apply), tail.collect(partial)) : tail.collect(partial)));
  }

  @Override
  public <R> Kind<F, R> foldLeft(R begin, Function2<? super R, ? super T, ? extends R> combinator) {
    return monad.flatMap(head, h -> tail.foldLeft(combinator.apply(begin, h), combinator));
  }

  @Override
  public <R> Kind<F, R> foldRight(Kind<F, ? extends R> begin, 
      Function2<? super T, ? super Kind<F, ? extends R>, ? extends Kind<F, ? extends R>> combinator) {
    return monad.flatMap(head, h -> tail.foldRight(combinator.apply(h, begin), combinator));
  }

  @Override
  public Kind<F, Boolean> exists(Matcher1<? super T> matcher) {
    return foldRight(monad.pure(false), (t, acc) -> matcher.match(t) ? monad.pure(true) : acc);
  }

  @Override
  public Kind<F, Boolean> forall(Matcher1<? super T> matcher) {
    return foldRight(monad.pure(true), (t, acc) -> matcher.match(t) ? acc : monad.pure(false));
  }

  @Override
  public <R> Stream<F, R> map(Function1<? super T, ? extends R> map) {
    return suspend(() -> cons(monad.map(head, map), suspend(() -> tail.map(map))));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<? super T, ? extends Kind<F, ? extends R>> mapper) {
    return suspend(() -> cons(monad.flatMap(head, mapper), suspend(() -> tail.mapEval(mapper))));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<? super T, ? extends Kind<Kind<Stream_, F>, ? extends R>> map) {
    return suspendF(() ->
        monad.map(
            monad.map(head, map.andThen(com.github.tonivade.purefun.stream.StreamOf::<F, R>narrowK)),
            s -> s.concat(tail.flatMap(map))));
  }

  @Override
  public Stream<F, T> repeat() {
    return concat(suspend(this::repeat));
  }

  @Override
  public Stream<F, T> intersperse(Kind<F, ? extends T> value) {
    return suspend(() -> cons(head, suspend(() -> cons(Kind.narrowK(value), tail.intersperse(value)))));
  }

  private <R> Stream<F, R> cons(Kind<F, R> h, Stream<F, R> t) {
    return new Cons<>(monad, h, t);
  }

  private <R> Stream<F, R> suspend(Producer<Stream<F, R>> stream) {
    return suspendF(stream.map(monad::<Stream<F, R>>pure));
  }

  private <R> Stream<F, R> suspendF(Producer<Kind<F, Stream<F, R>>> stream) {
    return new Suspend<>(monad, monad.defer(stream));
  }

  private Stream<F, T> empty() {
    return new Nil<>(monad);
  }
}
