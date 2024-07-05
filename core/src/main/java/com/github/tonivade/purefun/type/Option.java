/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.core.Precondition.checkNonNull;
import static com.github.tonivade.purefun.core.Producer.cons;
import static java.util.Objects.nonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nullable;
import com.github.tonivade.purefun.core.Applicable;
import com.github.tonivade.purefun.core.Bindable;
import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Function2;
import com.github.tonivade.purefun.core.Matcher1;
import com.github.tonivade.purefun.core.Producer;
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
public sealed interface Option<T> extends OptionOf<T>, Bindable<Option<?>, T>, Applicable<Option<?>, T> {

  static <T> Option<T> some(T value) {
    return new Some<>(value);
  }

  @SuppressWarnings("unchecked")
  static <T> Option<T> none() {
    return (Option<T>) None.INSTANCE;
  }

  static <T> Option<T> of(@Nullable T value) {
    return nonNull(value) ? some(value) : none();
  }

  static <T> Option<T> of(Producer<? extends T> producer) {
    return of(producer.get());
  }

  static <T> Option<T> from(Optional<T> optional) {
    return optional.map(Option::some).orElseGet(Option::none);
  }

  static <A, B, Z> Option<Z> map2(Option<A> optionA, Option<B> optionB, Function2<? super A, ? super B, ? extends Z> mapper) {
    return optionA.flatMap(a -> optionB.map(b -> mapper.apply(a, b)));
  }

  boolean isPresent();
  boolean isEmpty();

  @Override
  default <R> Option<R> map(Function1<? super T, ? extends R> mapper) {
    return switch (this) {
      case Some<T>(var value) -> some(mapper.apply(value));
      case None<T> n -> none();
    };
  }

  @Override
  default <R> Option<R> ap(Kind<Option<?>, ? extends Function1<? super T, ? extends R>> apply) {
    return apply.<Option<Function1<T, R>>>fix().flatMap(this::map);
  }

  @Override
  default <R> Option<R> flatMap(Function1<? super T, ? extends Kind<Option<?>, ? extends R>> map) {
    return switch (this) {
      case Some<T>(var value) -> map.andThen(OptionOf::<R>toOption).apply(value);
      case None<T> n -> none();
    };
  }

  default Option<T> ifPresent(Consumer1<? super T> consumer) {
    if (this instanceof Some<T>(var value)) {
      consumer.accept(value);
    }
    return this;
  }

  default Option<T> ifEmpty(Runnable run) {
    if (this instanceof None) {
      run.run();
    }
    return this;
  }

  default Option<T> filter(Matcher1<? super T> matcher) {
    if (this instanceof Some<T>(var value) && matcher.match(value)) {
      return this;
    }
    return none();
  }

  default Option<T> filterNot(Matcher1<? super T> matcher) {
    return filter(matcher.negate());
  }

  default Option<T> or(Producer<Kind<Option<?>, T>> orElse) {
    if (this instanceof None) {
      return orElse.andThen(OptionOf::toOption).get();
    }
    return this;
  }

  default Option<T> orElse(Kind<Option<?>, T> orElse) {
    return or(cons(orElse));
  }

  default T getOrElse(T value) {
    return getOrElse(cons(value));
  }

  @Nullable
  default T getOrElseNull() {
    return switch (this) {
      case None<T> n -> null;
      case Some<T>(var value) -> value;
    };
  }

  default T getOrElse(Producer<? extends T> producer) {
    return fold(producer, identity());
  }

  default T getOrElseThrow() {
    return getOrElseThrow(NoSuchElementException::new);
  }

  default <X extends Throwable> T getOrElseThrow(Producer<? extends X> producer) throws X {
    if (this instanceof Some<T>(var value)) {
      return value;
    }
    throw producer.get();
  }

  default <U> U fold(Producer<? extends U> orElse, Function1<? super T, ? extends U> mapper) {
    return switch (this) {
      case Some<T>(var value) -> mapper.apply(value);
      case None<T> n -> orElse.get();
    };
  }

  default Stream<T> stream() {
    return fold(Stream::empty, Stream::of);
  }

  default Sequence<T> sequence() {
    return fold(Sequence::emptyList, Sequence::listOf);
  }

  default Optional<T> toOptional() {
    return fold(Optional::empty, Optional::of);
  }

  default Either<Throwable, T> toEither() {
    return fold(() -> Either.left(new NoSuchElementException()), Either::right);
  }

  default Try<T> toTry() {
    return Try.fromEither(toEither());
  }

  record Some<T>(T value) implements Option<T>, Serializable {

    public Some {
      checkNonNull(value);
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
    public String toString() {
      return "Some(" + value + ")";
    }
  }

  final class None<T> implements Option<T>, Serializable {

    @Serial
    private static final long serialVersionUID = 7202112931010040785L;

    private static final None<?> INSTANCE = new None<>();

    private None() { }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean isPresent() {
      return false;
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

    @Serial
    private Object readResolve() {
      return INSTANCE;
    }
  }
}
