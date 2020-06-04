/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Equal;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Producer;
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
@HigherKind(sealed = true)
public interface Try<T> extends TryOf<T> {

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

  static <T> Try<T> of(Producer<T> supplier) {
    try {
      return success(supplier.get());
    } catch (Throwable error) {
      return failure(error);
    }
  }

  static <R> Try<R> fromEither(Either<Throwable, R> value) {
    return value.fold(Try::failure, Try::success);
  }

  Throwable getCause();
  boolean isSuccess();
  boolean isFailure();

  /**
   * Returns the value if available. If not, it throws {@code NoSuchElementException}
   * @return the wrapped value
   * @throws NoSuchElementException if value is not available
   */
  T get();

  default <R> Try<R> map(Function1<T, R> mapper) {
    if (isSuccess()) {
      return success(mapper.apply(get()));
    }
    return failure(getCause());
  }

  default <R> Try<R> flatMap(Function1<T, Try<R>> mapper) {
    if (isSuccess()) {
      return mapper.apply(get());
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

  default Try<T> filter(Matcher1<T> matcher) {
    return filterOrElse(matcher, () -> failure(new NoSuchElementException("filtered")));
  }

  default Try<T> filterNot(Matcher1<T> matcher) {
    return filter(matcher.negate());
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

  default Try<T> orElse(Try<T> orElse) {
    if (isFailure()) {
      return orElse;
    }
    return this;
  }

  default T getOrElse(T value) {
    return getOrElse(Producer.cons(value));
  }

  default T getOrElseNull() {
    return getOrElse(Producer.cons(null));
  }

  default T getOrElse(Producer<T> producer) {
    return fold(producer.asFunction(), identity());
  }

  default <X extends Throwable> T getOrElseThrow(Producer<X> producer) throws X {
    if (isSuccess()) {
      return get();
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

  default <E> Validation<E, T> toValidation(Function1<Throwable, E> map) {
    return fold(map.andThen(Validation::invalid), Validation::valid);
  }

  final class Success<T> implements SealedTry<T>, Serializable {

    private static final long serialVersionUID = -3934628369477099278L;

    private static final Equal<Success<?>> EQUAL = Equal.<Success<?>>of().comparing(Success::get);

    private final T value;

    private Success(T value) {
      this.value = checkNonNull(value);
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
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      return EQUAL.applyTo(this, obj);
    }

    @Override
    public String toString() {
      return "Success(" + value + ")";
    }
  }

  final class Failure<T> implements SealedTry<T>, Serializable {

    private static final long serialVersionUID = -8155444386075553318L;

    private static final Equal<Failure<?>> EQUAL =
        Equal.<Failure<?>>of().comparing(Failure::getMessage).comparingArray(Failure::getStackTrace);

    private final Throwable cause;

    private Failure(Throwable cause) {
      this.cause = checkNonNull(cause);
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
    public int hashCode() {
      return Objects.hash(cause.getMessage(), cause.getStackTrace());
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