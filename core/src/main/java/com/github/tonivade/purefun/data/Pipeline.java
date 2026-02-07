package com.github.tonivade.purefun.data;

import java.util.Objects;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Tuple2;

final class Pipeline<T, U> {

  private final Transducer<Object, T, U> xf;

  private Pipeline(Transducer<Object, T, U> xf) {
    this.xf = Objects.requireNonNull(xf);
  }

  public <A extends Iterable<U>> A run(Iterable<T> input, A init, Reducer<A, U> reducer) {
    return Transducer.run(xf.fix(), reducer, init, input);
  }

  @SuppressWarnings("unchecked")
  public static <T, U> Pipeline<T, U> narrowK(Pipeline<? super T, ? extends U> pipeline) {
    return (Pipeline<T, U>) pipeline;
  }

  public static <T> Pipeline<T, T> identity() {
    return new Pipeline<>(Transducer.identity());
  }

  public <V> Pipeline<T, V> map(Function1<? super U, ? extends V> f) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.map(f)));
  }

  public Pipeline<T, U> filter(Matcher1<? super U> p) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.filter(p)));
  }

  public <V> Pipeline<T, V> flatMap(Function1<? super U, ? extends Sequence<V>> f) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.flatMap(f)));
  }

  public Pipeline<T, U> take(int n) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.take(n)));
  }

  public Pipeline<T, U> drop(int n) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.drop(n)));
  }

  public Pipeline<T, Sequence<U>> tumbling(int size) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.tumbling(size)));
  }

  public Pipeline<T, Sequence<U>> sliding(int size) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.sliding(size)));
  }

  public Pipeline<T, U> distinct() {
    return new Pipeline<>(Transducer.chain(xf, Transducer.distinct()));
  }

  public Pipeline<T, Tuple2<Integer, U>> zipWithIndex() {
    return new Pipeline<>(Transducer.chain(xf, Transducer.zipWithIndex()));
  }

  public Pipeline<T, U> dropWhile(Matcher1<? super U> condition) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.dropWhile(condition)));
  }

  public Pipeline<T, U> takeWhile(Matcher1<? super U> condition) {
    return new Pipeline<>(Transducer.chain(xf, Transducer.takeWhile(condition)));
  }
}
