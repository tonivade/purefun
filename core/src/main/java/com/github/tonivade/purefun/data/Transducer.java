package com.github.tonivade.purefun.data;

import java.util.List;

import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Matcher1;

public interface Transducer<A, T, U> {

  @SuppressWarnings("unchecked")
  default <R extends Iterable<U>> Transducer<R, T, U> narrowK() {
    return (Transducer<R, T, U>) this;
  }

  Reducer<A, T> apply(Reducer<A, U> reducer);

  default <V> Transducer<A, T, V> andThen(Transducer<A, U, V> next) {
    return compose(this, next);
  }

  static <A, T, U, V> Transducer<A, T, V> compose(
      Transducer<A, T, U> t1,
      Transducer<A, U, V> t2) {
    return reducer -> t1.apply(t2.apply(reducer));
  }

  static <A, T, U> Transducer<A, T, U> map(Function1<? super T, ? extends U> f) {
    return reducer ->
        (acc, value) -> reducer.apply(acc, f.apply(value));
  }

  static <A, T, U> Transducer<A, T, U> flatMap(Function1<T, ? extends Sequence<U>> f) {
    return reducer ->
        (acc, value) -> {
            for (var u : f.apply(value)) {
                acc = reducer.apply(acc, u);
            }
            return acc;
        };
  }

  static <A, T> Transducer<A, T, T> filter(Matcher1<? super T> p) {
    return reducer ->
        (acc, value) -> p.test(value)
            ? reducer.apply(acc, value)
            : acc;
  }

  static <A extends Iterable<U>, T, U> A transduce(
      Transducer<A, T, U> xform,
      Reducer<A, U> reducer,
      A init,
      Iterable<T> input) {

    var r = xform.apply(reducer);
    var acc = init;

    for (var value : input) {
      acc = r.apply(acc, value);
    }
    return acc;
  }

  public static void main(String[] args) {
    Transducer<List<Integer>, Integer, Integer> xf =
        compose(
            filter((Integer x) -> x % 2 == 0),
            map(x -> x * 10)
            );

    Reducer<List<Integer>, Integer> append =
        (acc, value) -> {
          acc.add(value);
          return acc;
        };

        var result = transduce(
            xf,
            append,
            new java.util.ArrayList<Integer>(),
            List.of(1, 2, 3, 4, 5)
            );

        System.out.println(result); // [20, 40]
  }
}
