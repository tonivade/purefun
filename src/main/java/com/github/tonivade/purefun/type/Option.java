/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.unit;
import static com.github.tonivade.purefun.typeclasses.Eq.comparing;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Alternative;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Equal;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Semigroupal;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface Option<T> extends FlatMap1<Option.µ, T>, Filterable<T>, Holder<T> {

  final class µ implements Kind {}

  static <T> Option<T> some(T value) {
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  static <T> Option<T> none() {
    return (Option<T>) None.INSTANCE;
  }

  static <T> Option<T> narrowK(Higher1<Option.µ, T> hkt) {
    return (Option<T>) hkt;
  }

  static <T> Option<T> of(Producer<T> producer) {
    T value = producer.get();
    if (nonNull(value)) {
      return some(value);
    }
    return none();
  }

  static <T> Option<T> from(Optional<T> optional) {
    return optional.map(Option::some).orElseGet(Option::none);
  }

  boolean isPresent();
  boolean isEmpty();

  @Override
  default <R> Option<R> map(Function1<T, R> mapper) {
    if (isPresent()) {
      return some(mapper.apply(get()));
    }
    return none();
  }

  @Override
  default <R> Option<R> flatMap(Function1<T, ? extends Higher1<Option.µ, R>> map) {
    if (isPresent()) {
      return map.andThen(Option::narrowK).apply(get());
    }
    return none();
  }

  default Option<T> ifPresent(Consumer1<T> consumer) {
    if (isPresent()) {
      consumer.accept(get());
    }
    return this;
  }

  default Option<T> ifEmpty(Runnable run) {
    if (isEmpty()) {
      run.run();
    }
    return this;
  }

  @Override
  default Option<T> filter(Matcher1<T> matcher) {
    if (isPresent() && matcher.match(get())) {
      return this;
    }
    return none();
  }

  default Option<T> orElse(Option<T> orElse) {
    if (isEmpty()) {
      return orElse;
    }
    return this;
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.unit(value));
  }

  default T getOrElse(Producer<T> producer) {
    if (isEmpty()) {
      return producer.get();
    }
    return get();
  }

  default <X extends Throwable> T getOrElseThrow(Producer<X> producer) throws X {
    if (isEmpty()) {
      throw producer.get();
    }
    return get();
  }

  default <U> U fold(Producer<U> orElse, Function1<T, U> mapper) {
    if (isPresent()) {
      return mapper.apply(get());
    }
    return orElse.get();
  }

  default Stream<T> stream() {
    if (isPresent()) {
      return Stream.of(get());
    }
    return Stream.empty();
  }

  default Sequence<T> sequence() {
    if (isPresent()) {
      return ImmutableList.of(get());
    }
    return ImmutableList.empty();
  }

  default Optional<T> toOptional() {
    if (isPresent()) {
      return Optional.of(get());
    }
    return Optional.empty();
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V> Option<V> flatten() {
    try {
      return ((Option<Option<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  static <T> Eq<Higher1<Option.µ, T>> eq(Eq<T> eqSome) {
    return (a, b) -> Pattern2.<Option<T>, Option<T>, Boolean>build()
      .when((x, y) -> x.isPresent() && y.isPresent())
        .then((x, y) -> eqSome.eqv(x.get(), y.get()))
      .when((x, y) -> x.isEmpty() && y.isEmpty())
        .returns(true)
      .otherwise()
        .returns(false)
      .apply(narrowK(a), narrowK(b));
  }

  static Functor<Option.µ> functor() {
    return new Functor<Option.µ>() {

      @Override
      public <T, R> Option<R> map(Higher1<Option.µ, T> value, Function1<T, R> mapper) {
        return narrowK(value).map(mapper);
      }
    };
  }

  static Applicative<Option.µ> applicative() {
    return new Applicative<Option.µ>() {

      @Override
      public <T> Option<T> pure(T value) {
        return some(value);
      }

      @Override
      public <T, R> Option<R> ap(Higher1<Option.µ, T> value, Higher1<Option.µ, Function1<T, R>> apply) {
        return narrowK(value).flatMap(t -> narrowK(apply).map(f -> f.apply(t)));
      }
    };
  }

  static Alternative<Option.µ> alternative() {
    return new Alternative<Option.µ>() {
      @Override
      public <T> Option<T> zero() {
        return none();
      }

      @Override
      public <T> Option<T> combineK(Higher1<Option.µ, T> t1, Higher1<Option.µ, T> t2) {
        return narrowK(t1).fold(unit(narrowK(t2)), Option::some);
      }

      @Override
      public <T> Option<T> pure(T value) {
        return some(value);
      }

      @Override
      public <T, R> Option<R> ap(Higher1<Option.µ, T> value, Higher1<Option.µ, Function1<T, R>> apply) {
        return narrowK(value).flatMap(t -> narrowK(apply).map(f -> f.apply(t)));
      }
    };
  }

  static Monad<Option.µ> monad() {
    return new Monad<Option.µ>() {

      @Override
      public <T> Option<T> pure(T value) {
        return some(value);
      }

      @Override
      public <T, R> Option<R> flatMap(Higher1<Option.µ, T> value,
                                      Function1<T, ? extends Higher1<Option.µ, R>> map) {
        return narrowK(value).flatMap(map);
      }
    };
  }

  static MonadError<Option.µ, Nothing> monadError() {
    return new MonadError<Option.µ, Nothing>() {

      @Override
      public <T> Option<T> pure(T value) {
        return some(value);
      }

      @Override
      public <A> Option<A> raiseError(Nothing error) {
        return none();
      }

      @Override
      public <T, R> Option<R> flatMap(Higher1<Option.µ, T> value,
                                      Function1<T, ? extends Higher1<Option.µ, R>> map) {
        return narrowK(value).flatMap(map);
      }

      @Override
      public <A> Option<A> handleErrorWith(Higher1<Option.µ, A> value,
                                           Function1<Nothing, ? extends Higher1<Option.µ, A>> handler) {
        return narrowK(value).fold(() -> narrowK(handler.apply(nothing())), Option::some);
      }
    };
  }

  static Traverse<Option.µ> traverse() {
    return new Traverse<Option.µ>() {

      @Override
      public <G extends Kind, T, R> Higher1<G, Higher1<Option.µ, R>> traverse(
          Applicative<G> applicative, Higher1<Option.µ, T> value,
          Function1<T, ? extends Higher1<G, R>> mapper) {
        return narrowK(value).fold(
            () -> applicative.pure(none()),
            t -> applicative.map(mapper.apply(t), Option::some));
      }
    };
  }

  static Semigroupal<Option.µ> semigroupal() {
    return new Semigroupal<Option.µ>() {

      @Override
      public <A, B> Option<Tuple2<A, B>> product(Higher1<Option.µ, A> fa, Higher1<Option.µ, B> fb) {
        return narrowK(fa).flatMap(a -> narrowK(fb).map(b -> Tuple.of(a, b)));
      }
    };
  }

  OptionModule module();

  final class Some<T> implements Option<T>, Serializable {

    private static final long serialVersionUID = 7757183287962895363L;

    private final T value;

    private Some(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public boolean isPresent() {
      return true;
    }

    @Override
    public OptionModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .append(comparing(Option::get))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Some(" + value + ")";
    }
  }

  final class None<T> implements Option<T>, Serializable {

    private static final long serialVersionUID = 7202112931010040785L;

    private static final None<?> INSTANCE = new None<>();

    private None() { }

    @Override
    public T get() {
      throw new NoSuchElementException("get() in none");
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean isPresent() {
      return false;
    }

    @Override
    public OptionModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj;
    }

    @Override
    public String toString() {
      return "None";
    }
  }
}

interface OptionModule {

}