/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.cons;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Future;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;

public class EitherTTest {

  final Monad<Id.µ> monad = IdInstances.monad();

  @Test
  public void map() {
    EitherT<Id.µ, Nothing, String> right = EitherT.right(monad, "abc");

    EitherT<Id.µ, Nothing, String> map = right.map(String::toUpperCase);

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void flatMap() {
    EitherT<Id.µ, Nothing, String> right = EitherT.right(monad, "abc");

    EitherT<Id.µ, Nothing, String> map = right.flatMap(value -> EitherT.right(monad, value.toUpperCase()));

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void filterOrElse() {
    EitherT<Id.µ, Nothing, String> right = EitherT.right(monad, "abc");

    EitherT<Id.µ, Nothing, String> filter = right.filterOrElse(String::isEmpty, cons(Either.right("not empty")));
    EitherT<Id.µ, Nothing, String> orElse = EitherT.right(monad, "not empty");

    assertEquals(orElse.get(), filter.getOrElse("not empty"));
  }

  @Test
  public void left() {
    EitherT<Id.µ, Nothing, String> left = EitherT.left(monad, nothing());

    assertAll(
        () -> assertEquals(Id.of(true), left.isLeft()),
        () -> assertEquals(Id.of(false), left.isRight()),
        () -> assertEquals(Id.of("empty"), left.getOrElse("empty")));
  }

  @Test
  public void right() {
    EitherT<Id.µ, Nothing, String> right = EitherT.right(monad, "abc");

    assertAll(
        () -> assertEquals(Id.of(false), right.isLeft()),
        () -> assertEquals(Id.of(true), right.isRight()),
        () -> assertEquals(Id.of("abc"), right.getOrElse("empty")));
  }

  @Test
  public void mapK() {
    EitherT<IO.µ, Nothing, String> rightIo = EitherT.right(IOInstances.monad(), "abc");

    EitherT<Try.µ, Nothing, String> rightTry = rightIo.mapK(TryInstances.monad(), new IOToTryTransformer());

    assertEquals(Try.success("abc"), Try.narrowK(rightTry.get()));
  }

  @Test
  public void monadErrorFuture() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Higher1<EitherT.µ, Future.µ>, Throwable>, Throwable> monadError =
        EitherT.monadError(Future.monadError());

    Higher1<Higher1<Higher1<EitherT.µ, Future.µ>, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Higher1<EitherT.µ, Future.µ>, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Higher1<EitherT.µ, Future.µ>, Throwable>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Higher1<EitherT.µ, Future.µ>, Throwable>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<Higher1<EitherT.µ, Future.µ>, Throwable>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError.fix1(EitherT::narrowK).value().fix1(Future::narrowK).await()),
        () -> assertEquals(Try.success(Either.right("not an error")), handleError.fix1(EitherT::narrowK).value().fix1(Future::narrowK).await()),
        () -> assertEquals(Try.failure(error), ensureError.fix1(EitherT::narrowK).value().fix1(Future::narrowK).await()),
        () -> assertEquals(Try.success(Either.right("is not ok")), ensureOk.fix1(EitherT::narrowK).value().fix1(Future::narrowK).await()));
  }

  @Test
  public void monadErrorIO() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Higher1<EitherT.µ, Id.µ>, Throwable>, Throwable> monadError =
        EitherT.monadError(monad);

    Higher1<Higher1<Higher1<EitherT.µ, Id.µ>, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Higher1<EitherT.µ, Id.µ>, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Higher1<EitherT.µ, Id.µ>, Throwable>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Higher1<EitherT.µ, Id.µ>, Throwable>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<Higher1<EitherT.µ, Id.µ>, Throwable>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Id.of(Either.left(error)), raiseError.fix1(EitherT::narrowK).value()),
        () -> assertEquals(Id.of(Either.right("not an error")), handleError.fix1(EitherT::narrowK).value()),
        () -> assertEquals(Id.of(Either.left(error)), ensureError.fix1(EitherT::narrowK).value()),
        () -> assertEquals(Id.of(Either.right("is not ok")), ensureOk.fix1(EitherT::narrowK).value()));
  }
}
