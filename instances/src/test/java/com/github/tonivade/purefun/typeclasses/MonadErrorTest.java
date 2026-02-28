/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.core.Matcher1.always;
import static com.github.tonivade.purefun.core.Matcher1.is;
import static com.github.tonivade.purefun.core.Producer.cons;
import static com.github.tonivade.purefun.core.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.FutureOf;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.EIOOf;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIOOf;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.instances.EIOInstances;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.EvalInstances;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.PureIOInstances;
import com.github.tonivade.purefun.instances.TaskInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.instances.UIOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.type.EvalOf;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import org.junit.jupiter.api.Test;

public class MonadErrorTest {

  private final MonadError<Try<?>, Throwable> monadError = TryInstances.monadError();

  @Test
  public void recover() {
    Kind<Try<?>, String> recover =
        monadError.recover(Try.<String>failure("error"), PartialFunction1.of(always(), Throwable::toString));

    assertEquals(Try.success("java.lang.RuntimeException: error"), recover);
  }

  @Test
  public void attempRight() {
    Kind<Try<?>, Either<Throwable, String>> attempt = monadError.attempt(Try.success("hola mundo!"));

    assertEquals(Try.success(Either.right("hola mundo!")), attempt);
  }

  @Test
  public void attempLeft() {
    Exception error = new Exception("error");

    Kind<Try<?>, Either<Throwable, String>> attempt = monadError.attempt(Try.<String>failure(error));

    assertEquals(Try.success(Either.left(error)), attempt);
  }

  @Test
  public void ensureError() {
    Exception error = new Exception("error");

    Kind<Try<?>, String> ensure =
        monadError.ensure(Try.success("not ok"), cons(error), is("ok"));

    assertEquals(Try.failure(error), ensure);
  }

  @Test
  public void ensureOk() {
    Exception error = new Exception("error");

    Kind<Try<?>, String> ensure =
        monadError.ensure(Try.success("ok"), cons(error), is("ok"));

    assertEquals(Try.success("ok"), ensure);
  }

  @Test
  public void either() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Either<Throwable, ?>, Throwable> monadError = EitherInstances.<Throwable>monadError();

    Kind<Either<Throwable, ?>, String> pure = monadError.pure("is not ok");
    Kind<Either<Throwable, ?>, String> raiseError = monadError.raiseError(error);
    Kind<Either<Throwable, ?>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Kind<Either<Throwable, ?>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Kind<Either<Throwable, ?>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Either.left(error), raiseError),
        () -> assertEquals(Either.right("not an error"), handleError),
        () -> assertEquals(Either.left(error), ensureError),
        () -> assertEquals(Either.right("is not ok"), ensureOk));
  }

  @Test
  public void option() {
    MonadError<Option<?>, Unit> monadError = OptionInstances.monadError();

    Kind<Option<?>, String> pure = monadError.pure("is not ok");
    Kind<Option<?>, String> raiseError = monadError.raiseError(unit());
    Kind<Option<?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Option<?>, String> ensureOk = monadError.ensure(pure, Unit::unit, "is not ok"::equals);
    Kind<Option<?>, String> ensureError = monadError.ensure(pure, Unit::unit, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Option.none(), raiseError),
        () -> assertEquals(Option.some("not an error"), handleError),
        () -> assertEquals(Option.none(), ensureError),
        () -> assertEquals(Option.some("is not ok"), ensureOk));
  }

  @Test
  public void try_() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Try<?>, Throwable> monadError = TryInstances.monadError();

    Kind<Try<?>, String> pure = monadError.pure("is not ok");
    Kind<Try<?>, String> raiseError = monadError.raiseError(error);
    Kind<Try<?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Try<?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Try<?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), raiseError),
        () -> assertEquals(Try.success("not an error"), handleError),
        () -> assertEquals(Try.failure(error), ensureError),
        () -> assertEquals(Try.success("is not ok"), ensureOk));
  }

  @Test
  public void future() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Future<?>, Throwable> monadError = FutureInstances.monadError();

    Kind<Future<?>, String> pure = monadError.pure("is not ok");
    Kind<Future<?>, String> raiseError = monadError.raiseError(error);
    Kind<Future<?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Future<?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Future<?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.failure(error), FutureOf.toFuture(raiseError).await()),
        () -> assertEquals(Try.success("not an error"), FutureOf.toFuture(handleError).await()),
        () -> assertEquals(Try.failure(error), FutureOf.toFuture(ensureError).await()),
        () -> assertEquals(Try.success("is not ok"), FutureOf.toFuture(ensureOk).await()));
  }

  @Test
  public void eval() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Eval<?>, Throwable> monadError = EvalInstances.monadError();

    Kind<Eval<?>, String> pure = monadError.pure("is not ok");
    Kind<Eval<?>, String> raiseError = monadError.raiseError(error);
    Kind<Eval<?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Eval<?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Eval<?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> EvalOf.toEval(raiseError).value()),
        () -> assertEquals("not an error", EvalOf.toEval(handleError).value()),
        () -> assertThrows(RuntimeException.class, () -> EvalOf.toEval(ensureError).value()),
        () -> assertEquals("is not ok", EvalOf.toEval(ensureOk).value()));
  }

  @Test
  public void io() {
    RuntimeException error = new RuntimeException("error");
    MonadError<IO<?>, Throwable> monadError = IOInstances.monadError();

    Kind<IO<?>, String> pure = monadError.pure("is not ok");
    Kind<IO<?>, String> raiseError = monadError.raiseError(error);
    Kind<IO<?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<IO<?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<IO<?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> raiseError.fix(IOOf::toIO).unsafeRunSync()),
        () -> assertEquals("not an error", handleError.fix(IOOf::toIO).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> ensureError.fix(IOOf::toIO).unsafeRunSync()),
        () -> assertEquals("is not ok", ensureOk.fix(IOOf::toIO).unsafeRunSync()));
  }

  @Test
  public void uio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<UIO<?>, Throwable> monadError = UIOInstances.monadError();

    Kind<UIO<?>, String> pure = monadError.pure("is not ok");
    Kind<UIO<?>, String> raiseError = monadError.raiseError(error);
    Kind<UIO<?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<UIO<?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<UIO<?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertThrows(RuntimeException.class, () -> UIOOf.toUIO(raiseError).unsafeRunSync()),
        () -> assertEquals("not an error", UIOOf.toUIO(handleError).unsafeRunSync()),
        () -> assertThrows(RuntimeException.class, () -> UIOOf.toUIO(ensureError).unsafeRunSync()),
        () -> assertEquals("is not ok", UIOOf.toUIO(ensureOk).unsafeRunSync()));
  }

  @Test
  public void eio() {
    RuntimeException error = new RuntimeException("error");
    MonadError<EIO<Throwable, ?>, Throwable> monadError = EIOInstances.monadThrow();

    Kind<EIO<Throwable, ?>, String> pure = monadError.pure("is not ok");
    Kind<EIO<Throwable, ?>, String> raiseError = monadError.raiseError(error);
    Kind<EIO<Throwable, ?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<EIO<Throwable, ?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<EIO<Throwable, ?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), EIOOf.toEIO(raiseError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), EIOOf.toEIO(handleError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>left(error), EIOOf.toEIO(ensureError).safeRunSync()),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), EIOOf.toEIO(ensureOk).safeRunSync()));
  }

  @Test
  public void task() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Task<?>, Throwable> monadError = TaskInstances.monadThrow();

    Kind<Task<?>, String> pure = monadError.pure("is not ok");
    Kind<Task<?>, String> raiseError = monadError.raiseError(error);
    Kind<Task<?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<Task<?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<Task<?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Try.<String>failure(error), TaskOf.toTask(raiseError).safeRunSync()),
        () -> assertEquals(Try.success("not an error"), TaskOf.toTask(handleError).safeRunSync()),
        () -> assertEquals(Try.<String>failure(error), TaskOf.toTask(ensureError).safeRunSync()),
        () -> assertEquals(Try.success("is not ok"), TaskOf.toTask(ensureOk).safeRunSync()));
  }

  @Test
  public void PureIO() {
    RuntimeException error = new RuntimeException("error");
    MonadError<PureIO<Void, Throwable, ?>, Throwable> monadError = PureIOInstances.monadThrow();

    Kind<PureIO<Void, Throwable, ?>, String> pure = monadError.pure("is not ok");
    Kind<PureIO<Void, Throwable, ?>, String> raiseError = monadError.raiseError(error);
    Kind<PureIO<Void, Throwable, ?>, String> handleError = monadError.handleError(raiseError, e -> "not an error");
    Kind<PureIO<Void, Throwable, ?>, String> ensureOk = monadError.ensure(pure, () -> error, "is not ok"::equals);
    Kind<PureIO<Void, Throwable, ?>, String> ensureError = monadError.ensure(pure, () -> error, "is ok?"::equals);

    assertAll(
        () -> assertEquals(Either.<Throwable, String>left(error), PureIOOf.toPureIO(raiseError).provide(null)),
        () -> assertEquals(Either.<Throwable, String>right("not an error"), PureIOOf.toPureIO(handleError).provide(null)),
        () -> assertEquals(Either.<Throwable, String>left(error), PureIOOf.toPureIO(ensureError).provide(null)),
        () -> assertEquals(Either.<Throwable, String>right("is not ok"), PureIOOf.toPureIO(ensureOk).provide(null)));
  }
}
