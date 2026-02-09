/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.core.Operator2;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Reducer.Step;
import com.github.tonivade.purefun.type.Option;
import java.util.Comparator;

/**
 * A Finisher is a function that takes a Transducer and produces a result of type A.
 * It is used to run a Transducer on an input collection and produce a final result.
 *
 * @param <A> the type of the result produced by the Finisher
 * @param <T> the type of input elements processed by the Transducer
 * @param <U> the type of output elements produced by the Transducer
 */
@FunctionalInterface
public interface Finisher<A, T, U> {

  /**
   * Applies the given Transducer to produce a result of type A.
   *
   * @param transducer the Transducer to apply
   * @return the result produced by applying the Transducer
   */
  A apply(Transducer<A, T, U> transducer);

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces a result of type A.
   *
   * @param input the input collection to process
   * @param init a Producer that provides the initial value of type A
   * @param append a Function2 that takes the current accumulated value of type A and an element of type E,
   *               and produces a new accumulated value of type A
   * @param <A> the type of the result produced by the Finisher
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and produces a result of type A
   */
  static <A, E, R> Finisher<A, E, R> of(
      Iterable<? extends E> input, Producer<A> init, Function2<? super A, ? super R, ? extends A> append) {
    return of(input, init, append, a -> a);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces a result of type A.
   *
   * @param input the input collection to process
   * @param init a Producer that provides the initial value of type A
   * @param append a Function2 that takes the current accumulated value of type A and an element of type E,
   *               and produces a new accumulated value of type A
   * @param onComplete an Operator1 that takes the final accumulated value of type A and produces the final result of type A
   * @param <A> the type of the result produced by the Finisher
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and produces a result of type A
   */
  static <A, E, R> Finisher<A, E, R> of(
      Iterable<? extends E> input, Producer<A> init, Function2<? super A, ? super R, ? extends A> append, Operator1<A> onComplete) {
    return xf -> run(init.get(), input, xf.apply((acc, e) -> Step.more(append.apply(acc, e))), onComplete);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and performs the given action on each output element.
   *
   * @param input the input collection to process
   * @param action the action to perform on each output element
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and performs the given action on each output element
   */
  static <E, R> Finisher<Unit, E, R> forEach(Iterable<? extends E> input, Consumer1<? super R> action) {
    return of(input, Unit::unit, (acc, e) -> {
      action.accept(e);
      return acc;
    });
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces a String by joining the
   * string representations of the output elements with the given separator.
   *
   * @param input the input collection to process
   * @param separator the separator to use between the string representations of the output elements
   * @param prefix the prefix to add at the beginning of the resulting string
   * @param suffix the suffix to add at the end of the resulting string
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and produces a String by joining the
   *  string representations of the output elements with the given separator
   */
  static <E, R> Finisher<String, E, R> join(Iterable<? extends E> input, String separator, String prefix, String suffix) {
    return of(input, String::new, (acc, e) -> acc.isEmpty() ? acc + e : acc + separator + e, acc -> prefix + acc + suffix);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an Option containing the
   * first output element, or None if there are no output elements.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements produced by the Transducer
   * @return a Finisher that runs the given Transducer on the input collection and produces an Option containing the
   *  first output element, or None if there are no output elements
   */
  static <E, R> Finisher<Option<R>, E, R> findFirst(Iterable<? extends E> input) {
    return xf -> run(Option.none(), input, xf.apply((acc, e) -> Step.done(Option.some(e))));
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableMap that groups
   * the output elements by the keys produced by the given selector function.
   *
   * @param input the input collection to process
   * @param selector the function to produce keys for grouping the output elements
   * @param <K> the type of keys produced by the selector function
   * @param <E> the type of input elements to process
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableMap that groups
   *  the output elements by the keys produced by the given selector function
   */
  static <K, E> Finisher<ImmutableMap<K, ImmutableList<E>>, E, E> groupBy(
        Iterable<? extends E> input, Function1<? super E, ? extends K> selector) {
    return of(input, ImmutableMap::empty,
      (acc, e) -> acc.merge(selector.apply(e), listOf(e), ImmutableList::appendAll));
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableArray of type R.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableArray of type R
   */
  static <E, R> Finisher<ImmutableArray<R>, E, R> toImmutableArray(Iterable<? extends E> input) {
    return of(input, ImmutableArray::empty, ImmutableArray::append);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableList of type R.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableList of type R
   */
  static <E, R> Finisher<ImmutableList<R>, E, R> toImmutableList(Iterable<? extends E> input) {
    return of(input, ImmutableList::empty, ImmutableList::append);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableMap of type K to V,
   * where the keys and values are produced by the given keySelector and valueSelector functions, respectively.
   * If there are duplicate keys, the values are merged using the given merger function.
   *
   * @param input the input collection to process
   * @param keySelector the function to produce keys for the ImmutableMap
   * @param valueSelector the function to produce values for the ImmutableMap
   * @param merger the function to merge values for duplicate keys in the ImmutableMap
   * @param <K> the type of keys in the resulting ImmutableMap
   * @param <V> the type of values in the resulting ImmutableMap
   * @param <E> the type of input elements to process
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableMap of type K to V,
   *  where the keys and values are produced by the given keySelector and valueSelector functions, respectively.
   *  If there are duplicate keys, the values are merged using the given merger function.
   */
  static <E, K, V> Finisher<ImmutableMap<K, V>, E, E> toImmutableMap(
      Iterable<? extends E> input, Function1<? super E, ? extends K> keySelector, Function1<? super E, ? extends V> valueSelector, Operator2<V> merger) {
    return of(input, ImmutableMap::empty,
      (acc, e) -> acc.merge(keySelector.apply(e), valueSelector.apply(e), merger));
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableTreeMap of type K to V,
   * where the keys and values are produced by the given keySelector and valueSelector functions, respectively.
   * If there are duplicate keys, the values are merged using the given merger function.
   * The keys in the resulting ImmutableTreeMap are ordered according to their natural ordering.
   *
   * @param input the input collection to process
   * @param keySelector the function to produce keys for the ImmutableTreeMap
   * @param valueSelector the function to produce values for the ImmutableTreeMap
   * @param merger the function to merge values for duplicate keys in the ImmutableTreeMap
   * @param <K> the type of keys in the resulting ImmutableTreeMap
   * @param <V> the type of values in the resulting ImmutableTreeMap
   * @param <E> the type of input elements to process
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableTreeMap of type K to V,
   *  where the keys and values are produced by the given keySelector and valueSelector functions, respectively.
   *  If there are duplicate keys, the values are merged using the given merger function.
   */
  static <E, K, V> Finisher<ImmutableTreeMap<K, V>, E, E> toImmutableTreeMap(
      Iterable<? extends E> input,
      Function1<? super E, ? extends K> keySelector, Function1<? super E, ? extends V> valueSelector, Operator2<V> merger) {
    return of(input, ImmutableTreeMap::empty,
      (acc, e) -> acc.merge(keySelector.apply(e), valueSelector.apply(e), merger));
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableTreeMap of type K to V,
   * where the keys and values are produced by the given keySelector and valueSelector functions, respectively.
   * If there are duplicate keys, the values are merged using the given merger function.
   * The keys in the resulting ImmutableTreeMap are ordered according to the given Comparator.
   *
   * @param input the input collection to process
   * @param comparator the Comparator to use for ordering the keys in the ImmutableTreeMap
   * @param keySelector the function to produce keys for the ImmutableTreeMap
   * @param valueSelector the function to produce values for the ImmutableTreeMap
   * @param merger the function to merge values for duplicate keys in the ImmutableTreeMap
   * @param <K> the type of keys in the resulting ImmutableTreeMap
   * @param <V> the type of values in the resulting ImmutableTreeMap
   * @param <E> the type of input elements to process
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableTreeMap of type K to V,
   *  where the keys and values are produced by the given keySelector and valueSelector functions, respectively.
   *  If there are duplicate keys, the values are merged using the given merger function.
   */
  static <E, K, V> Finisher<ImmutableTreeMap<K, V>, E, E> toImmutableTreeMap(
      Iterable<? extends E> input, Comparator<? super K> comparator,
      Function1<? super E, ? extends K> keySelector, Function1<? super E, ? extends V> valueSelector, Operator2<V> merger) {
    return of(input, () -> ImmutableTreeMap.empty(comparator),
      (acc, e) -> acc.merge(keySelector.apply(e), valueSelector.apply(e), merger));
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableSet of type R.
   *
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableSet of type R
   */
  static <E, R> Finisher<ImmutableSet<R>, E, R> toImmutableSet(Iterable<? extends E> input) {
    return of(input, ImmutableSet::empty, ImmutableSet::append);
  }

  /**
   * Creates a Finisher that runs the given Transducer on the input collection and produces an ImmutableTree of type R.
   *
   * @param comparator the Comparator to use for ordering the elements in the ImmutableTree
   * @param input the input collection to process
   * @param <E> the type of input elements to process
   * @param <R> the type of output elements
   * @return a Finisher that runs the given Transducer on the input collection and produces an ImmutableTree of type R
   */
  static <E, R> Finisher<ImmutableTree<R>, E, R> toImmutableTree(
      Comparator<? super R> comparator, Iterable<? extends E> input) {
    return of(input, () -> ImmutableTree.empty(comparator), ImmutableTree::append);
  }

  private static <A, T> A run(A init, Iterable<? extends T> input, Reducer<A, T> reducer) {
    return run(init, input, reducer, a -> a);
  }

  private static <A, T> A run(A init, Iterable<? extends T> input, Reducer<A, T> reducer, Operator1<A> complete) {
    var acc = init;
    for (var value : input) {
      var step = reducer.apply(acc, value);
      acc = step.value();
      if (step instanceof Step.Done) {
        break;
      }
    }
    return complete.apply(acc);
  }
}
