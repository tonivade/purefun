/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

public final class Cons<F extends Kind<F, ?>, T> implements PureStream<F, T> {

  private final MonadDefer<F> monad;
  private final Kind<F, T> head;
  private final PureStream<F, T> tail;

  Cons(MonadDefer<F> monad, Kind<F, T> head, PureStream<F, T> tail) {
    this.monad = checkNonNull(monad);
    this.head = checkNonNull(head);
    this.tail = checkNonNull(tail);
  }

  @Override
  public Kind<F, Option<T>> headOption() {
    return monad.map(head, Option::some);
  }

  @Override
  public Kind<F, Option<Tuple2<Kind<F, T>, PureStream<F, T>>>> split() {
    return monad.pure(Option.some(Tuple.of(head, tail)));
  }

  @Override
  public PureStream<F, T> concat(PureStream<F, ? extends T> other) {
    return suspend(() -> cons(head, tail.concat(other)));
  }

  @Override
  public PureStream<F, T> append(Kind<F, ? extends T> other) {
    return suspend(() -> cons(head, tail.append(other)));
  }

  @Override
  public PureStream<F, T> prepend(Kind<F, ? extends T> other) {
    return suspend(() -> cons(Kind.narrowK(other), tail.prepend(head)));
  }

  @Override
  public PureStream<F, T> take(int n) {
    return n > 0 ? suspend(() -> cons(head, tail.take(n - 1))) : empty();
  }

  @Override
  public PureStream<F, T> drop(int n) {
    return n > 0 ? suspend(() -> tail.drop(n - 1)) : this;
  }

  @Override
  public PureStream<F, T> takeWhile(Matcher1<? super T> matcher) {
    return suspendF(() -> monad.map(head,
        t -> matcher.match(t) ? cons(head, tail.takeWhile(matcher)) : empty()));
  }

  @Override
  public PureStream<F, T> dropWhile(Matcher1<? super T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                tail.dropWhile(matcher) : this));
  }

  @Override
  public PureStream<F, T> filter(Matcher1<? super T> matcher) {
    return suspendF(() ->
            monad.map(head, t -> matcher.match(t) ?
                cons(head, tail.filter(matcher)) : tail.filter(matcher)));
  }

  @Override
  public <R> PureStream<F, R> collect(PartialFunction1<? super T, ? extends R> partial) {
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
  public <R> PureStream<F, R> map(Function1<? super T, ? extends R> map) {
    return suspend(() -> cons(monad.map(head, map), suspend(() -> tail.map(map))));
  }

  @Override
  public <R> PureStream<F, R> mapEval(Function1<? super T, ? extends Kind<F, ? extends R>> mapper) {
    return suspend(() -> cons(monad.flatMap(head, mapper), suspend(() -> tail.mapEval(mapper))));
  }

  @Override
  public <R> PureStream<F, R> flatMap(Function1<? super T, ? extends Kind<PureStream<F, ?>, ? extends R>> map) {
    return suspendF(() ->
        monad.map(
            monad.map(head, map.andThen(Kind::<PureStream<F, R>>fix)),
            s -> s.concat(tail.flatMap(map))));
  }

  @Override
  public PureStream<F, T> repeat() {
    return concat(suspend(this::repeat));
  }

  @Override
  public PureStream<F, T> intersperse(Kind<F, ? extends T> value) {
    return suspend(() -> cons(head, suspend(() -> cons(Kind.narrowK(value), tail.intersperse(value)))));
  }

  private <R> PureStream<F, R> cons(Kind<F, R> h, PureStream<F, R> t) {
    return new Cons<>(monad, h, t);
  }

  private <R> PureStream<F, R> suspend(Producer<PureStream<F, R>> stream) {
    return suspendF(stream.map(monad::<PureStream<F, R>>pure));
  }

  private <R> PureStream<F, R> suspendF(Producer<Kind<F, PureStream<F, R>>> stream) {
    return new Suspend<>(monad, monad.defer(stream));
  }

  private PureStream<F, T> empty() {
    return new Nil<>(monad);
  }
}
