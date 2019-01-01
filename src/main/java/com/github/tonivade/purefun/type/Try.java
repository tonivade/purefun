/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.typeclasses.Eq.comparing;
import static com.github.tonivade.purefun.typeclasses.Eq.comparingArray;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tonivade.purefun.CheckedProducer;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Holder;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Equal;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Traverse;

public interface Try<T> extends FlatMap1<Try.µ, T>, Filterable<T>, Holder<T> {

  final class µ implements Kind {}

  static <T> Try<T> success(T value) {
    return new Success<>(value);
  }

  static <T> Try<T> failure(String message) {
    return failure(new Exception(message));
  }

  static <T> Try<T> failure() {
    return failure(new Exception());
  }

  static <T> Try<T> failure(Throwable error) {
    return new Failure<>(error);
  }

  static <T> Try<T> of(CheckedProducer<T> supplier) {
    try {
      return success(supplier.get());
    } catch (Throwable error) {
      return failure(error);
    }
  }

  static <T> Try<T> narrowK(Higher1<Try.µ, T> hkt) {
    return (Try<T>) hkt;
  }

  Throwable getCause();
  boolean isSuccess();
  boolean isFailure();

  @Override
  default <R> Try<R> map(Function1<T, R> mapper) {
    if (isSuccess()) {
      return success(mapper.apply(get()));
    }
    return failure(getCause());
  }

  @Override
  default <R> Try<R> flatMap(Function1<T, ? extends Higher1<Try.µ, R>> mapper) {
    if (isSuccess()) {
      return mapper.andThen(Try::narrowK).apply(get());
    }
    return failure(getCause());
  }

  default Try<T> onFailure(Consumer1<Throwable> consumer) {
    if (isFailure()) {
      consumer.accept(getCause());
    }
    return this;
  }

  default Try<T> onSuccess(Consumer1<T> consumer) {
    if (isSuccess()) {
      consumer.accept(get());
    }
    return this;
  }

  default Try<T> recover(Function1<Throwable, T> mapper) {
    if (isFailure()) {
      return Try.of(() -> mapper.apply(getCause()));
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  default <X extends Throwable> Try<T> recoverWith(Class<X> type, Function1<X, T> mapper) {
    if (isFailure()) {
      Throwable cause = getCause();
      if (type.isAssignableFrom(cause.getClass())) {
        return Try.of(() -> mapper.apply((X) getCause()));
      }
    }
    return this;
  }

  @Override
  default Try<T> filter(Matcher1<T> matcher) {
    return filterOrElse(matcher, () -> failure(new NoSuchElementException("filtered")));
  }

  default Try<T> filterOrElse(Matcher1<T> matcher, Producer<Try<T>> producer) {
    if (isFailure() || matcher.match(get())) {
      return this;
    }
    return producer.get();
  }

  default <U> U fold(Function1<Throwable, U> failureMapper, Function1<T, U> successMapper) {
    if (isSuccess()) {
      return successMapper.apply(get());
    }
    return failureMapper.apply(getCause());
  }

  default T orElse(T value) {
    return orElse(Producer.unit(value));
  }

  default T orElse(Producer<T> producer) {
    if (isSuccess()) {
      return get();
    }
    return producer.get();
  }

  default <X extends Throwable> T orElseThrow(Producer<X> producer) throws X {
    if (isSuccess()) {
      return get();
    }
    throw producer.get();
  }

  default Stream<T> stream() {
    if (isSuccess()) {
      return Stream.of(get());
    }
    return Stream.empty();
  }

  default Sequence<T> sequence() {
    if (isSuccess()) {
      return ImmutableList.of(get());
    }
    return ImmutableList.empty();
  }

  default Either<Throwable, T> toEither() {
    if (isSuccess()) {
      return Either.right(get());
    }
    return Either.left(getCause());
  }

  default <E> Validation<E, T> toValidation(Function1<Throwable, E> map) {
    if (isSuccess()) {
      return Validation.valid(get());
    }
    return Validation.invalid(map.apply(getCause()));
  }

  default Option<T> toOption() {
    if (isSuccess()) {
      return Option.some(get());
    }
    return Option.none();
  }

  @Override
  @SuppressWarnings("unchecked")
  default <V> Try<V> flatten() {
    try {
      return ((Try<Try<V>>) this).flatMap(identity());
    } catch (ClassCastException e) {
      throw new UnsupportedOperationException("cannot be flattened");
    }
  }

  static <T> Eq<Higher1<Try.µ, T>> eq(Eq<T> eqSuccess) {
    final Eq<Throwable> eqFailure = Eq.throwable();
    return (a, b) -> Pattern2.<Try<T>, Try<T>, Boolean>build()
      .when((x, y) -> x.isFailure() && y.isFailure())
        .then((x, y) -> eqFailure.eqv(x.getCause(), y.getCause()))
      .when((x, y) -> x.isSuccess() && y.isSuccess())
        .then((x, y) -> eqSuccess.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(narrowK(a), narrowK(b));
  }

  static Functor<Try.µ> functor() {
    return new Functor<Try.µ>() {

      @Override
      public <T, R> Try<R> map(Higher1<Try.µ, T> value, Function1<T, R> mapper) {
        return narrowK(value).map(mapper);
      }
    };
  }

  static Applicative<Try.µ> applicative() {
    return new Applicative<Try.µ>() {

      @Override
      public <T> Try<T> pure(T value) {
        return success(value);
      }

      @Override
      public <T, R> Try<R> ap(Higher1<Try.µ, T> value, Higher1<Try.µ, Function1<T, R>> apply) {
        return narrowK(value).flatMap(t -> narrowK(apply).map(f -> f.apply(t)));
      }
    };
  }

  static Monad<Try.µ> monad() {
    return new Monad<Try.µ>() {

      @Override
      public <T> Try<T> pure(T value) {
        return success(value);
      }

      @Override
      public <T, R> Try<R> flatMap(Higher1<Try.µ, T> value,
                                   Function1<T, ? extends Higher1<Try.µ, R>> map) {
        return narrowK(value).flatMap(map);
      }
    };
  }

  static MonadError<Try.µ, Throwable> monadError() {
    return new MonadError<Try.µ, Throwable>() {

      @Override
      public <U> Try<U> pure(U value) {
        return success(value);
      }

      @Override
      public <A> Try<A> raiseError(Throwable error) {
        return failure(error);
      }

      @Override
      public <T, R> Try<R> flatMap(Higher1<Try.µ, T> value,
                                   Function1<T, ? extends Higher1<Try.µ, R>> map) {
        return narrowK(value).flatMap(map);
      }

      @Override
      public <A> Try<A> handleErrorWith(Higher1<Try.µ, A> value,
                                        Function1<Throwable, ? extends Higher1<Try.µ, A>> handler) {
        return narrowK(value).fold(handler.andThen(Try::narrowK), Try::success);
      }
    };
  }

  static Traverse<Try.µ> traverse() {
    return new Traverse<Try.µ>() {

      @Override
      public <G extends Kind, T, R> Higher1<G, Higher1<Try.µ, R>> traverse(
          Applicative<G> applicative, Higher1<Try.µ, T> value,
          Function1<T, ? extends Higher1<G, R>> mapper) {
        return narrowK(value).fold(
            t -> applicative.pure(failure(t)),
            t -> applicative.map(mapper.apply(t), Try::success));
      }
    };
  }

  TryModule module();

  final class Success<T> implements Try<T>, Serializable {

    private static final long serialVersionUID = -3934628369477099278L;

    private final T value;

    private Success(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public T get() {
      return value;
    }

    @Override
    public Throwable getCause() {
      throw new NoSuchElementException("success doesn't have any cause");
    }

    @Override
    public TryModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .append(comparing(Try::get))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Success(" + value + ")";
    }
  }

  final class Failure<T> implements Try<T>, Serializable {

    private static final long serialVersionUID = -8155444386075553318L;

    private final Throwable cause;

    private Failure(Throwable cause) {
      this.cause = requireNonNull(cause);
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public T get() {
      throw new NoSuchElementException("failure doesn't have any value");
    }

    @Override
    public Throwable getCause() {
      return cause;
    }

    private String getMessage() {
      return cause.getMessage();
    }

    private StackTraceElement[] getStackTrace() {
      return cause.getStackTrace();
    }

    @Override
    public TryModule module() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
      return Objects.hash(cause.getMessage(), cause.getStackTrace());
    }

    @Override
    public boolean equals(Object obj) {
      return Equal.of(this)
          .append(comparing(Failure::getMessage))
          .append(comparingArray(Failure::getStackTrace))
          .applyTo(obj);
    }

    @Override
    public String toString() {
      return "Failure(" + cause + ")";
    }
  }
}

interface TryModule {

}