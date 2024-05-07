/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nullable;
import com.github.tonivade.purefun.core.Applicable;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Equal;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Recoverable;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;

/**
 * <p>This type represents the success or failure of a computation. It has two possible values:</p>
 * <ul>
 *   <li>{@code Try.success()}: when the computation is successful</li>
 *   <li>{@code Try.failure()}: when the result of a computation is an exception</li>
 * </ul>
 * <p>You can obtain an {@code Either<Throwable, T>} from {@code Try<T>}</p>
 * <p><strong>Note:</strong> it's serializable</p>
 * @param <T> the wrapped value
 */
@HigherKind
public sealed interface Try<T> extends TryOf<T>, Bindable<Try<?>, T>, Applicable<Try<?>, T> {

  static <T> Try<T> success(T value) {
    return new Success<>(value);
  }

  static <T> Try<T> failure(String message) {
    return failure(new RuntimeException(message));
  }

  static <T> Try<T> failure() {
    return failure(new RuntimeException());
  }

  static <T> Try<T> failure(Throwable error) {
    return new Failure<>(error);
  }

  static <T> Try<T> noSuchElementException() {
    return failure(new NoSuchElementException());
  }

  static <T> Try<T> illegalArgumentException() {
    return failure(new IllegalArgumentException());
  }

  static <T> Try<T> illegalStateException() {
    return failure(new IllegalStateException());
  }

  static <T> Try<T> unsupportedOperationException() {
    return failure(new UnsupportedOperationException());
  }

  static <T> Try<T> noSuchElementException(String cause) {
    return failure(new NoSuchElementException(cause));
  }

  static <T> Try<T> illegalArgumentException(String cause) {
    return failure(new IllegalArgumentException(cause));
  }

  static <T> Try<T> illegalStateException(String cause) {
    return failure(new IllegalStateException(cause));
  }

  static <T> Try<T> unsupportedOperationException(String cause) {
    return failure(new UnsupportedOperationException(cause));
  }

  static <T> Try<T> of(Producer<? extends T> supplier) {
    try {
      return success(supplier.get());
    } catch (Throwable error) {
      return failure(error);
    }
  }

  static <R> Try<R> from(Throwable error, R value) {
    return error != null ? Try.failure(error) : Try.success(value);
  }

  static <R> Try<R> fromEither(Either<? extends Throwable, ? extends R> value) {
    return value.fold(Try::failure, Try::success);
  }

  static <A, B, Z> Try<Z> map2(Try<A> tryA, Try<B> tryB, Function2<? super A, ? super B, ? extends Z> mapper) {
    return tryA.flatMap(a -> tryB.map(b -> mapper.apply(a, b)));
  }

  T getOrElseThrow();
  Throwable getCause();
  boolean isSuccess();
  boolean isFailure();

  @Override
  default <R> Try<R> map(Function1<? super T, ? extends R> mapper) {
    return flatMap(mapper.liftTry());
  }

  @Override
  default <R> Try<R> ap(Kind<Try<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return apply.fix(TryOf::toTry).flatMap(this::map);
  }

  default Try<T> mapError(Function1<? super Throwable, ? extends Throwable> mapper) {
    return switch (this) {
      case Failure(var cause) -> failure(mapper.apply(cause));
      case Success<T> s -> this;
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  default <R> Try<R> flatMap(Function1<? super T, ? extends Kind<Try<?>, ? extends R>> mapper) {
    return switch (this) {
      case Success<T>(var value) -> mapper.andThen(TryOf::<R>toTry).apply(value);
      case Failure<T> f -> (Try<R>) this;
    };
  }

  default Try<T> onFailure(Consumer1<? super Throwable> consumer) {
    if (this instanceof Failure(var cause)) {
      consumer.accept(cause);
    }
    return this;
  }

  default Try<T> onSuccess(Consumer1<? super T> consumer) {
    if (this instanceof Success<T>(var value)) {
      consumer.accept(value);
    }
    return this;
  }

  default Try<T> recover(Function1<? super Throwable, ? extends T> mapper) {
    if (this instanceof Failure(var cause)) {
      return Try.of(() -> mapper.apply(cause));
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  default <X extends Throwable> Try<T> recoverWith(Class<X> type, Function1<? super X, ? extends T> mapper) {
    if (this instanceof Failure(var cause) && type.isAssignableFrom(cause.getClass())) {
      return Try.of(() -> mapper.apply((X) cause));
    }
    return this;
  }

  default Try<T> filter(Matcher1<? super T> matcher) {
    return filterOrElse(matcher, () -> noSuchElementException("filtered"));
  }

  default Try<T> filterNot(Matcher1<? super T> matcher) {
    return filter(matcher.negate());
  }

  default Try<T> filterOrElse(Matcher1<? super T> matcher, Producer<? extends Kind<Try<?>, ? extends T>> producer) {
    if (this instanceof Failure) {
      return this;
    }
    if (this instanceof Success<T>(var value) && matcher.match(value)) {
      return this;
    }
    return producer.andThen(TryOf::<T>toTry).get();
  }

  default <U> U fold(Function1<? super Throwable, ? extends U> failureMapper, Function1<? super T, ? extends U> successMapper) {
    return switch (this) {
      case Success<T>(var value) -> successMapper.apply(value);
      case Failure(var cause) -> failureMapper.apply(cause);
    };
  }

  default Try<T> or(Producer<Kind<Try<?>, T>> orElse) {
    if (this instanceof Failure) {
      return orElse.andThen(TryOf::toTry).get();
    }
    return this;
  }

  default Try<T> orElse(Kind<Try<?>, T> orElse) {
    return or(Producer.cons(orElse));
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  @Nullable
  default T getOrElseNull() {
    return switch (this) {
      case Success<T>(var value) -> value;
      case Failure<T> f -> null;
    };
  }

  default T getOrElse(Producer<? extends T> producer) {
    return fold(producer.asFunction(), identity());
  }

  default <X extends Throwable> T getOrElseThrow(Producer<? extends X> producer) throws X {
    if (this instanceof Success<T>(var value)) {
      return value;
    }
    throw producer.get();
  }

  default Stream<T> stream() {
    return fold(cons(Stream.empty()), Stream::of);
  }

  default Sequence<T> sequence() {
    return fold(cons(ImmutableList.empty()), ImmutableList::of);
  }

  default Option<T> toOption() {
    return fold(cons(Option.none()), Option::some);
  }

  default Either<Throwable, T> toEither() {
    return fold(Either::left, Either::right);
  }

  default <E> Validation<E, T> toValidation(Function1<? super Throwable, ? extends E> map) {
    return fold(map.andThen(Validation::invalid), Validation::valid);
  }

  record Success<T>(T value) implements Try<T>, Serializable {

    public Success {
      checkNonNull(value);
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
    public T getOrElseThrow() {
      return value;
    }

    @Override
    public Throwable getCause() {
      throw new NoSuchElementException("success doesn't have any cause");
    }

    @Override
    public String toString() {
      return "Success(" + value + ")";
    }
  }

  record Failure<T>(Throwable cause) implements Try<T>, Recoverable, Serializable {

    private static final Equal<Failure<?>> EQUAL =
        Equal.<Failure<?>>of().comparing(Failure::getMessage).comparingArray(Failure::getStackTrace);

    public Failure {
      checkNonNull(cause);
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
    public T getOrElseThrow() {
      return sneakyThrow(cause);
    }

    @Override
    public Throwable getCause() {
      return cause;
    }

    private Option<String> getMessage() {
      return Option.of(cause.getMessage());
    }

    private StackTraceElement[] getStackTrace() {
      return cause.getStackTrace();
    }

    @Override
    public int hashCode() {
      return Objects.hash(cause.getMessage(), Arrays.hashCode(cause.getStackTrace()));
    }

    @Override
    public boolean equals(Object obj) {
      return EQUAL.applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "Failure(" + cause + ")";
    }
  }
}