/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;

import static com.github.tonivade.purefun.Function1.identity;
import static java.util.Objects.requireNonNull;

public final class Optional<S, A> {

  private final Function1<S, Function1<A, S>> set;
  private final Function1<S, Either<S, A>> getOrModify;

  private Optional(Function1<S, Function1<A, S>> set, Function1<S, Either<S, A>> getOrModify) {
    this.set = requireNonNull(set);
    this.getOrModify = requireNonNull(getOrModify);
  }

  public static <S, A> Optional<S, A> of(Function2<S, A, S> set, Function1<S, Either<S, A>> getOrModify) {
    return new Optional<>(set.curried(), getOrModify);
  }

  public Function1<A, S> set(S target) {
    return set.apply(target);
  }

  public S set(S target, A value) {
    return set(target).apply(value);
  }

  public Either<S, A> getOrModify(S target) {
    return getOrModify.apply(target);
  }

  public Option<A> getOption(S target) {
    return getOrModify(target).toOption();
  }

  public Operator1<S> lift(Operator1<A> mapper) {
    return target -> modify(target, mapper);
  }

  public S modify(S target, Operator1<A> mapper) {
    return getOrModify(target).fold(identity(), value -> set(target, mapper.apply(value)));
  }

  public Option<S> modifyOption(S target, Operator1<A> mapper) {
    return getOption(target).map(value -> set(target, mapper.apply(value)));
  }

  public <B> Optional<S, B> compose(Optional<A, B> other) {
    return new Optional<>(
        target -> value -> this.modify(target, a -> other.set(a, value)),
        target -> this.getOrModify(target).flatMap(a -> other.getOrModify(a).bimap(b -> this.set(target, b), identity())) );
  }
}
