package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.type.Eval.later;
import static java.util.Objects.requireNonNull;

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

  Stream<F, T> head();
  Stream<F, T> tail();

  Stream<F, T> concat(Stream<F, T> other);

  <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator);
  <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator);

  @Override
  <R> Stream<F, R> map(Function1<T, R> map);

  @Override
  <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map);

  <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper);

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
  public Stream<F, T> head() {
    return new Cons<>(monad, comonad, head);
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
    System.out.println("cons => foldLeft");
    return monad.flatMap(head, h -> tail.foldLeft(combinator.apply(begin, h), combinator));
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    System.out.println("cons => foldRight");
    return later(() -> monad.flatMap(head, h -> tail.foldRight(combinator.apply(h, begin), combinator).value()));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    System.out.println("cons => map");
    return new Cons<>(monad, comonad, monad.map(head, map), new Defer<>(monad, later(() -> tail.map(map))));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    System.out.println("cons => mapEval");
    return new Cons<>(monad, comonad, monad.flatMap(head, mapper), new Defer<>(monad, later(() -> tail.mapEval(mapper))));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    System.out.println("cons => flatMap");
    return new Defer<>(monad,
        later(() -> comonad.extract(
            monad.map(
                monad.map(head, map.andThen(Stream::narrowK)::apply), s -> s.concat(tail.flatMap(map))))));
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
  public Stream<F, T> head() {
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
    System.out.println("defer => foldLeft");
    return lazyStream.flatMap(s -> later(() -> s.foldLeft(begin, combinator))).value();
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    System.out.println("defer => foldRight");
    return lazyStream.flatMap(s -> s.foldRight(begin, combinator));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    System.out.println("defer => map");
    return new Defer<>(monad, lazyStream.map(s -> s.map(map)));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    System.out.println("defer => mapEval");
    return new Defer<>(monad, lazyStream.map(s -> s.mapEval(mapper)));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<µ, F, R>> map) {
    System.out.println("defer => flatMap");
    return new Defer<>(monad, lazyStream.map(s -> s.flatMap(map)));
  }
}

class Nil<F extends Kind, T> implements Stream<F, T> {

  private Monad<F> monad;

  Nil(Monad<F> monad) {
    this.monad = requireNonNull(monad);
  }

  @Override
  public Stream<F, T> head() {
    return this;
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
    System.out.println("nil => foldLeft");
    return monad.pure(begin);
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    System.out.println("nil => foldRight");
    return begin.map(monad::pure);
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    System.out.println("nil => map");
    return new Nil<>(monad);
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    System.out.println("nil => mapEval");
    return new Nil<>(monad);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    System.out.println("nil => flatMap");
    return new Nil<>(monad);
  }
}