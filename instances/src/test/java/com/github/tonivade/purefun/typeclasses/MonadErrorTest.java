/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.Matcher1.always;
import static com.github.tonivade.purefun.Matcher1.is;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.EIOInstances;
import com.github.tonivade.purefun.instances.EvalInstances;
import com.github.tonivade.purefun.instances.TaskInstances;
import com.github.tonivade.purefun.instances.UIOInstances;
import com.github.tonivade.purefun.instances.ZIOInstances;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.type.Eval_;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.concurrent.Future_;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Either_;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.Try_;

public class MonadErrorTest {

  private final MonadError<Try_, Throwable> monadError = TryInstances.monadError();

  @Test
  public void recover() {
    Higher1<Try_, String> recover =
        monadError.recover(Try.<String>failure("error"), PartialFunction1.of(always(), Throwable::toString));

    assertEquals(Try.success("java.lang.Exception: error"), recover);
  }

  @Test
  public void attempRight() {
    Higher1<Try_, Either<Throwable, String>> attempt = monadError.attempt(Try.success("hola mundo!"));

    assertEquals(Try.success(Either.right("hola mundo!")), attempt);
  }

  @Test
  public void attempLeft() {
    Exception error = new Exception("error");

    Higher1<Try_, Either<Throwable, String>> attempt = monadError.attempt(Try.<String>failure(error));

    assertEquals(Try.success(Either.left(error)), attempt);
  }

  @Test
  public void ensureError() {
    Exception error = new Exception("error");

    Higher1<Try_, String> ensure =
        monadError.ensure(Try.success("not ok"), cons(error), is("ok"));

    assertEquals(Try.failure(error), ensure);
  }

  @Test
  public void ensureOk() {
    Exception error = new Exception("error");

    Higher1<Try_, String> ensure =
        monadError.ensure(Try.success("ok"), cons(error), is("ok"));

    assertEquals(Try.success("ok"), ensure);
  }

  @Test
  public void either() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Either_, Throwable>, Throwable> monadError = EitherInstances.<Throwable>monadError();

    Higher1<Higher1<Either_, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Either_, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Either_, Throwable>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Either_, Throwable>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<Either_, Throwable>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Either.left(error), raiseError),
        () -> assertEquals(Either.right("not an error"), handleError),
        () -> assertEquals(Either.left(error), ensureError),
        () -> assertEquals(Either.right("is not ok"), ensureOk));
  }

  @Test
  public void option() {
    MonadError<Option_, Unit> monadError = OptionInstances.monadError();

    Higher1<Option_, String> pure = monadError.pure("is not ok");
    Higher1<Option_, String> raiseError = monadError.raiseError(unit());
    Higher1<Option_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Option_, String> ensureOk = monadError.ensure(pure, Unit::unit, "is not ok"::equals);
    Higher1<Option_, String> ensureError = monadError.ensure(pure, Unit::unit, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Option.none(), raiseError),
        () -> assertEquals(Option.some("not an error"), handleError),
        () -> assertEquals(Option.none(), ensureError),
        () -> assertEquals(Option.some("is not ok"), ensureOk));
  }

  @Test
  public void try_() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Try_, Throwable> monadError = TryInstances.monadError();

    Higher1<Try_, String> pure = monadError.pure("is not ok");
    Higher1<Try_, String> raiseError = monadError.raiseError(error);
    Higher1<Try_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Try_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Try_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError),
        () -> assertEquals(Try.success("not an error"), handleError),
        () -> assertEquals(Try.failure(error), ensureError),
        () -> assertEquals(Try.success("is not ok"), ensureOk));
  }

  @Test
  public void future() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Future_, Throwable> monadError = FutureInstances.monadError();

    Higher1<Future_, String> pure = monadError.pure("is not ok");
    Higher1<Future_, String> raiseError = monadError.raiseError(error);
    Higher1<Future_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Future_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Future_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), Future_.narrowK(raiseError).await()),
        () -> assertEquals(Try.success("not an error"), Future_.narrowK(handleError).await()),
        () -> assertEquals(Try.failure(error), Future_.narrowK(ensureError).await()),
        () -> assertEquals(Try.success("is not ok"), Future_.narrowK(ensureOk).await()));
  }

  @Test
  public void eval() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Eval_, Throwable> monadError = EvalInstances.monadError();

    Higher1<Eval_, String> pure = monadError.pure("is not ok");
    Higher1<Eval_, String> raiseError = monadError.raiseError(error);
    Higher1<Eval_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Eval_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Eval_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> Eval_.narrowK(raiseError).value()),
        () -> assertEquals("not an error", Eval_.narrowK(handleError).value()),
        () -> assertThrows(RuntimeException.class, () -> Eval_.narrowK(ensureError).value()),
        () -> assertEquals("is not ok", Eval_.narrowK(ensureOk).value()));
  }

  @Test
  public void io() {
    RuntimeException error = new RuntimeException("error");
    MonadError<IO_, Throwable> monadError = IOInstances.monadError();

    Higher1<IO_, String> pure = monadError.pure("is not ok");
    Higher1<IO_, String> raiseError = monadError.raiseError(error);
    Higher1<IO_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<IO_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<IO_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> IO_.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", IO_.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> IO_.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", IO_.narrowK(ensureOk).unsafeRunSync()));
  }

  @Test
  public void uio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<UIO_, Throwable> monadError = UIOInstances.monadError();

    Higher1<UIO_, String> pure = monadError.pure("is not ok");
    Higher1<UIO_, String> raiseError = monadError.raiseError(error);
    Higher1<UIO_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<UIO_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<UIO_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> UIO_.narrowK(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", UIO_.narrowK(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> UIO_.narrowK(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", UIO_.narrowK(ensureOk).unsafeRunSync()));
  }

  @Test
  public void eio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<EIO_, Throwable>, Throwable> monadError = EIOInstances.monadThrow();

    Higher1<Higher1<EIO_, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<EIO_, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<EIO_, Throwable>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<EIO_, Throwable>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Higher1<EIO_, Throwable>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), EIO_.narrowK(raiseError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), EIO_.narrowK(handleError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>left(error), EIO_.narrowK(ensureError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), EIO_.narrowK(ensureOk).safeRunSync()));
  }

  @Test
  public void task() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Task_, Throwable> monadError = TaskInstances.monadThrow();

    Higher1<Task_, String> pure = monadError.pure("is not ok");
    Higher1<Task_, String> raiseError = monadError.raiseError(error);
    Higher1<Task_, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Task_, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Task_, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.<String>failure(error), Task_.narrowK(raiseError).safeRunSync()),
        () -> assertEquals(Try.success("not an error"), Task_.narrowK(handleError).safeRunSync()),
        () -> assertEquals(Try.<String>failure(error), Task_.narrowK(ensureError).safeRunSync()),
        () -> assertEquals(Try.success("is not ok"), Task_.narrowK(ensureOk).safeRunSync()));
  }

  @Test
  public void zio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Higher1<ZIO_, Nothing>, Throwable>, Throwable> monadError = ZIOInstances.monadThrow();

    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Higher1<Higher1<Higher1<ZIO_, Nothing>, Throwable>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), ZIO_.narrowK(raiseError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), ZIO_.narrowK(handleError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>left(error), ZIO_.narrowK(ensureError).provide(nothing())),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), ZIO_.narrowK(ensureOk).provide(nothing())));
  }
}
