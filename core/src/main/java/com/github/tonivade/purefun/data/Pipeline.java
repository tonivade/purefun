package com.github.tonivade.purefun.data;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Tuple2;

final class Pipeline<T, U> {

  private final Transducer<Object, T, U> xf;

  private Pipeline(Transducer<Object, T, U> xf) {
    this.xf = xf;
  }

  public <A extends Iterable<U>> A run(Iterable<T> input, A init, Reducer<A, U> reducer) {
    return Transducer.run(xf.fix(), reducer, init, input);
  }

  @SuppressWarnings("unchecked")
  public static <T, U> Pipeline<T, U> narrowK(Pipeline<? super T, ? extends U> pipeline) {
    return (Pipeline<T, U>) pipeline;
  }

  public static <T, U, V> Pipeline<T, V> chain(Pipeline<T, U> p1, Pipeline<U, V> p2) {
    return new Pipeline<>(reducer -> p1.xf.apply(p2.xf.apply(reducer)));
  }

  public static <T> Pipeline<T, T> identity() {
    return new Pipeline<>((Transducer<Object, T, T>) r -> r);
  }

  public static <T, U> Pipeline<T, U> map(Function1<? super T, ? extends U> f) {
    return new Pipeline<>(Transducer.map(f));
  }

  public static <T> Pipeline<T, T> filter(Matcher1<? super T> p) {
    return new Pipeline<>(Transducer.filter(p));
  }

  public static <T, U> Pipeline<T, U> flatMap(Function1<? super T, ? extends Sequence<U>> f) {
    return new Pipeline<>(Transducer.flatMap(f));
  }

  public static <T> Pipeline<T, T> take(int n) {
    return new Pipeline<>(Transducer.take(n));
  }

  public static <T> Pipeline<T, T> drop(int n) {
    return new Pipeline<>(Transducer.drop(n));
  }

  public static <T> Pipeline<T, Sequence<T>> tumbling(int size) {
    return new Pipeline<>(Transducer.tumbling(size));
  }

  public static <T> Pipeline<T, Sequence<T>> sliding(int size) {
    return new Pipeline<>(Transducer.sliding(size));
  }

  public static <T> Pipeline<T, T> distinct() {
    return new Pipeline<>(Transducer.distinct());
  }

  public static <T> Pipeline<T, Tuple2<Integer, T>> zipWithIndex() {
    return new Pipeline<>(Transducer.zipWithIndex());
  }

  public static <T> Pipeline<T, T> dropWhile(Matcher1<? super T> condition) {
    return new Pipeline<>(Transducer.dropWhile(condition));
  }

  public static <T> Pipeline<T, T> takeWhile(Matcher1<? super T> condition) {
    return new Pipeline<>(Transducer.takeWhile(condition));
  }
}
