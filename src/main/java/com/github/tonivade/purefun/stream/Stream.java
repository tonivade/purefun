package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.type.Eval.later;
import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;

import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface Stream<F extends Kind, T> extends FlatMap2<Stream.µ, F, T> {

  final class µ implements Kind {}

  Higher1<F, T> head();
  Stream<F, T> tail();

  Stream<F, T> concat(Stream<F, T> other);

  <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator);

  @Override
  <R> Stream<F, R> map(Function1<T, R> map);

  @Override
  <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<µ, F, R>> map);

  static <F extends Kind, T> Stream<F, T> pure(Monad<F> monad, Comonad<F> comonad, T value) {
    return new Cons<>(monad, comonad, monad.pure(value));
  }

  static <F extends Kind, T> Stream<F, T> narrowK(Higher1<Higher1<Stream.µ, F>, T> hkt) {
    return (Stream<F, T>) hkt;
  }
}

class Cons<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Comonad<F> comonad;
  private final Higher1<F, T> head;
  private final Stream<F, T> tail;

  Cons(Monad<F> monad, Comonad<F> comonad, Higher1<F, T> head) {
    this(monad, comonad, head, new Nil<>(monad));
  }

  Cons(Monad<F> monad, Comonad<F> comonad, Higher1<F, T> head, Stream<F, T> tail) {
    this.monad = requireNonNull(monad);
    this.comonad = requireNonNull(comonad);
    this.head = requireNonNull(head);
    this.tail = requireNonNull(tail);
  }

  @Override
  public Higher1<F, T> head() {
    return head;
  }

  @Override
  public Stream<F, T> tail() {
    return tail;
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return new Defer<>(monad, later(() -> new Cons<>(monad, comonad, head, tail.concat(other))));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(head, h -> tail.foldLeft(combinator.apply(begin, h), combinator));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return new Cons<>(monad, comonad, monad.map(head, map), new Defer<>(monad, later(() -> tail.map(map))));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return comonad.extract(monad.map(head, h -> map.andThen(Stream::narrowK).apply(h).concat(tail.flatMap(map))));
  }
}

class Defer<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Eval<Stream<F, T>> lazyStream;

  Defer(Monad<F> monad, Eval<Stream<F, T>> stream) {
    this.monad = requireNonNull(monad);
    this.lazyStream = requireNonNull(stream);
  }

  @Override
  public Higher1<F, T> head() {
    return lazyStream.value().head();
  }

  @Override
  public Stream<F, T> tail() {
    return new Defer<>(monad, lazyStream.map(s -> s.tail()));
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return new Defer<>(monad, lazyStream.map(s -> s.concat(other)));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return lazyStream.flatMap(s -> later(() -> s.foldLeft(begin, combinator))).value();
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return new Defer<>(monad, lazyStream.map(s -> s.map(map)));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<µ, F, R>> map) {
    return new Defer<>(monad, lazyStream.map(s -> s.flatMap(map)));
  }
}

class Nil<F extends Kind, T> implements Stream<F, T> {

  private Monad<F> monad;

  Nil(Monad<F> monad) {
    this.monad = requireNonNull(monad);
  }

  @Override
  public Higher1<F, T> head() {
    throw new NoSuchElementException();
  }

  @Override
  public Stream<F, T> tail() {
    return this;
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return other;
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.pure(begin);
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return new Nil<>(monad);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return new Nil<>(monad);
  }
}