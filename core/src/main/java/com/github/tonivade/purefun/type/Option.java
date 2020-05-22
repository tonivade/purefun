/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Producer.cons;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
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
 * <p>This type represents the presence or absence of a value, similar to {@link java.util.Optional}</p>
 * <p>There are two possible values:</p>
 * <ul>
 *   <li>{@code Option.none()}: that represent the absence of a value</li>
 *   <li>{@code Option.some(value)}: that represent the presence of a value</li>
 * </ul>
 * <p><strong>Note:</strong> it's serializable</p>
 * @param <T> the wrapped value
 */
@HigherKind
public interface Option<T> extends OptionOf<T> {

  static <T> Option<T> some(T value) {
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  static <T> Option<T> none() {
    return (Option<T>) None.INSTANCE;
  }

  static <T> Option<T> of(T value) {
    return nonNull(value) ? some(value) : none();
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

  /**
   * Returns the value if available. If not, it throws {@code NoSuchElementException}
   * @return the wrapped value
   * @throws NoSuchElementException if value is not available
   */
  T get();

  default <R> Option<R> map(Function1<T, R> mapper) {
    if (isPresent()) {
      return some(mapper.apply(get()));
    }
    return none();
  }

  default <R> Option<R> flatMap(Function1<T, Option<R>> map) {
    if (isPresent()) {
      return map.apply(get());
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

  default Option<T> filter(Matcher1<T> matcher) {
    if (isPresent() && matcher.match(get())) {
      return this;
    }
    return none();
  }

  default Option<T> filterNot(Matcher1<T> matcher) {
    return filter(matcher.negate());
  }

  default Option<T> orElse(Option<T> orElse) {
    if (isEmpty()) {
      return orElse;
    }
    return this;
  }

  default T getOrElse(T value) {
    return getOrElse(cons(value));
  }

  default T getOrElseNull() {
    return getOrElse(cons(null));
  }

  default T getOrElse(Producer<T> producer) {
    return fold(producer, identity());
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
    return fold(Stream::empty, Stream::of);
  }

  default Sequence<T> sequence() {
    return fold(ImmutableList::empty, ImmutableList::of);
  }

  default Optional<T> toOptional() {
    return fold(Optional::empty, Optional::of);
  }

  default Either<Throwable, T> toEither() {
    return fold(() -> Either.left(new NoSuchElementException()), Either::right);
  }

  OptionModule module();

  final class Some<T> implements Option<T>, Serializable {

    private static final long serialVersionUID = 7757183287962895363L;

    private static final Equal<Some<?>> EQUAL = Equal.<Some<?>>of().comparing(Option::get);

    private final T value;

    private Some(T value) {
      this.value = checkNonNull(value);
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
      return EQUAL.applyTo(this, obj);
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

interface OptionModule {}
