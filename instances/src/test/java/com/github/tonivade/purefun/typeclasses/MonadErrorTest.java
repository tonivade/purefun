/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Matcher1.always;
import static com.github.tonivade.purefun.Matcher1.is;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.cons;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class MonadErrorTest {

  private final MonadError<Try.µ, Throwable> monadError = TryInstances.monadError();

  @Test
  public void recover() {
    Higher1<Try.µ, String> recover =
        monadError.recover(Try.<String>failure("error").kind1(), PartialFunction1.of(always(), Throwable::toString));

    assertEquals(Try.success("java.lang.Exception: error"), recover);
  }

  @Test
  public void attempRight() {
    Higher1<Try.µ, Either<Throwable, String>> attemp = monadError.attemp(Try.success("hola mundo!").kind1());

    assertEquals(Try.success(Either.right("hola mundo!")), attemp);
  }

  @Test
  public void attempLeft() {
    Exception error = new Exception("error");

    Higher1<Try.µ, Either<Throwable, String>> attemp = monadError.attemp(Try.<String>failure(error).kind1());

    assertEquals(Try.success(Either.left(error)), attemp);
  }

  @Test
  public void ensureError() {
    Exception error = new Exception("error");

    Higher1<Try.µ, String> ensure =
        monadError.ensure(Try.success("not ok").kind1(), cons(error), is("ok"));

    assertEquals(Try.failure(error), ensure);
  }

  @Test
  public void ensureOk() {
    Exception error = new Exception("error");

    Higher1<Try.µ, String> ensure =
        monadError.ensure(Try.success("ok").kind1(), cons(error), is("ok"));

    assertEquals(Try.success("ok"), ensure);
  }

  @Test
  public void either() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Either.µ, Throwable>, Throwable> monadError = EitherInstances.<Throwable>monadError();

    Higher1<Higher1<Either.µ, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Either.µ, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Either.µ, Throwable>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Either.µ, Throwable>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<Either.µ, Throwable>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Either.left(error), raiseError),
        () -> assertEquals(Either.right("not an error"), handleError),
        () -> assertEquals(Either.left(error), ensureError),
        () -> assertEquals(Either.right("is not ok"), ensureOk));
  }

  @Test
  public void option() {
    MonadError<Option.µ, Nothing> monadError = OptionInstances.monadError();

    Higher1<Option.µ, String> pure = monadError.pure("is not ok");
    Higher1<Option.µ, String> raiseError = monadError.raiseError(nothing());
    Higher1<Option.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Option.µ, String> ensureOk = monadError.ensure(pure, Nothing::nothing, "is not ok"::equals);
    Higher1<Option.µ, String> ensureError = monadError.ensure(pure, Nothing::nothing, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Option.none(), raiseError),
        () -> assertEquals(Option.some("not an error"), handleError),
        () -> assertEquals(Option.none(), ensureError),
        () -> assertEquals(Option.some("is not ok"), ensureOk));
  }

  @Test
  public void try_() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Try.µ, Throwable> monadError = TryInstances.monadError();

    Higher1<Try.µ, String> pure = monadError.pure("is not ok");
    Higher1<Try.µ, String> raiseError = monadError.raiseError(error);
    Higher1<Try.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Try.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Try.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError),
        () -> assertEquals(Try.success("not an error"), handleError),
        () -> assertEquals(Try.failure(error), ensureError),
        () -> assertEquals(Try.success("is not ok"), ensureOk));
  }

  @Test
  public void future() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Future.µ, Throwable> monadError = FutureInstances.monadError();

    Higher1<Future.µ, String> pure = monadError.pure("is not ok");
    Higher1<Future.µ, String> raiseError = monadError.raiseError(error);
    Higher1<Future.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Future.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Future.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), Future.narrowK(raiseError).await()),
        () -> assertEquals(Try.success("not an error"), Future.narrowK(handleError).await()),
        () -> assertEquals(Try.failure(error), Future.narrowK(ensureError).await()),
        () -> assertEquals(Try.success("is not ok"), Future.narrowK(ensureOk).await()));
  }

  @Test
  public void io() {
    RuntimeException error = new RuntimeException("error");
    MonadError<IO.µ, Throwable> monadError = IOInstances.monadError();

    Higher1<IO.µ, String> pure = monadError.pure("is not ok");
    Higher1<IO.µ, String> raiseError = monadError.raiseError(error);
    Higher1<IO.µ, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<IO.µ, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<IO.µ, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> IO.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", IO.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> IO.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", IO.narrowK(ensureOk).unsafeRunSync()));
  }
}
