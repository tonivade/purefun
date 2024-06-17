/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.type.Either;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MonadDeferTest {

  private MonadDefer<IO<?>> ioMonadDefer = Instances.monadDefer();
  private MonadDefer<PureIO<Void, Throwable, ?>> PureIOMonadDefer = Instances.monadDefer();
  private MonadDefer<EitherT<IO<?>, Throwable, ?>> eitherTMonadDefer =
      new Instance<EitherT<IO<?>, Throwable, ?>>(){}.monadDefer(ioMonadDefer);
  private MonadDefer<OptionT<IO<?>, ?>> optionTMonadDefer =
      new Instance<OptionT<IO<?>, ?>>(){}.monadDefer(ioMonadDefer);

  private AutoCloseable resource = mock();

  @Test
  public void ioLater(@Mock Producer<String> task) {
    when(task.get()).thenReturn("hola toni");

    Kind<IO<?>, String> later = ioMonadDefer.later(task);

    String result = later.<IO<String>>fix().unsafeRunSync();

    assertEquals("hola toni", result);
    verify(task).get();
  }

  @Test
  public void ioBracket() throws Exception {
    Kind<IO<?>, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.pure("done"));

    String result = bracket.<IO<String>>fix().unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void ioBracketAcquireError() throws Exception {
    Kind<IO<?>, String> bracket =
        ioMonadDefer.bracket(IO.<AutoCloseable>raiseError(new IllegalStateException()), r -> IO.pure("done"));

    assertThrows(IllegalStateException.class, () -> bracket.<IO<String>>fix().unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void ioBracketUseError() throws Exception {
    Kind<IO<?>, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.<String>raiseError(new UnsupportedOperationException()));

    assertThrows(UnsupportedOperationException.class, () -> bracket.<IO<String>>fix().unsafeRunSync());

    verify(resource).close();
  }

  @Test
  public void eitherTBracket() throws Exception {
    Kind<EitherT<IO<?>, Throwable, ?>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.<IO<?>, Throwable, AutoCloseable>right(Instances.monad(), resource),
                                                r -> EitherT.<IO<?>, Throwable, String>right(Instances.monad(), "done"));

    String result = bracket.<EitherT<IO<?>, Throwable, String>>fix().get().<IO<String>>fix().unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void eitherTBracketAcquireError() throws Exception {
    Kind<EitherT<IO<?>, Throwable, ?>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.<IO<?>, Throwable, AutoCloseable>left(Instances.monad(), new IllegalStateException()),
                                                r -> EitherT.<IO<?>, Throwable, String>right(Instances.monad(), "done"));

    assertThrows(IllegalStateException.class,
                 () -> bracket.<EitherT<IO<?>, Throwable, String>>fix().value().<IO<String>>fix().unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void eitherTBracketUseError() throws Exception {
    Kind<EitherT<IO<?>, Throwable, ?>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.<IO<?>, Throwable, AutoCloseable>right(Instances.monad(), resource),
                                                r -> EitherT.<IO<?>, Throwable, String>left(Instances.monad(), new UnsupportedOperationException()));

    Either<Throwable, String> unsafeRunSync = bracket.<EitherT<IO<?>, Throwable, String>>fix().value().<IO<Either<Throwable, String>>>fix().unsafeRunSync();

    assertTrue(unsafeRunSync.getLeft() instanceof UnsupportedOperationException);
    verify(resource).close();
  }

  @Test
  public void optionTBracket() throws Exception {
    Kind<OptionT<IO<?>, ?>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(Instances.<IO<?>>monad(), resource),
                                  r -> OptionT.some(Instances.<IO<?>>monad(), "done"));

    String result = bracket.<OptionT<IO<?>, String>>fix().getOrElseThrow().<IO<String>>fix().unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  @Disabled
  public void optionTBracketAcquireError() throws Exception {
    Kind<OptionT<IO<?>, ?>, String> bracket =
        optionTMonadDefer.bracket(OptionT.<IO<?>, AutoCloseable>none(Instances.<IO<?>>monad()),
                                  r -> OptionT.some(Instances.<IO<?>>monad(), "done"));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.<OptionT<IO<?>, String>>fix().getOrElseThrow().<IO<String>>fix().unsafeRunSync());

    assertEquals("could not acquire resource", error.getMessage());
    verify(resource, never()).close();
  }

  @Test
  @Disabled
  public void optionTBracketUseError() throws Exception {
    Kind<OptionT<IO<?>, ?>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(Instances.<IO<?>>monad(), resource),
                                  r -> OptionT.<IO<?>, String>none(Instances.<IO<?>>monad()));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.<OptionT<IO<?>, String>>fix().getOrElseThrow().<IO<String>>fix().unsafeRunSync());

    assertEquals("get() in none", error.getMessage());
    verify(resource).close();
  }

  @Test
  public void pureIOBracket() throws Exception {
    Kind<PureIO<Void, Throwable, ?>, String> bracket =
        PureIOMonadDefer.bracket(PureIO.<Void, Throwable, AutoCloseable>pure(resource),
                              r -> PureIO.<Void, Throwable, String>pure("done"));

    Either<Throwable, String> result = bracket.<PureIO<Void, Throwable, String>>fix().provide(null);

    assertEquals(Either.right("done"), result);
    verify(resource).close();
  }

  @Test
  public void pureIOBracketAcquireError() throws Exception {
    Kind<PureIO<Void, Throwable, ?>, String> bracket =
        PureIOMonadDefer.bracket(PureIO.<Void, Throwable, AutoCloseable>raiseError(new IllegalStateException()),
                              r -> PureIO.<Void, Throwable, String>pure("done"));

    Either<Throwable, String> result = bracket.<PureIO<Void, Throwable, String>>fix().provide(null);

    assertTrue(result.isLeft());
    verify(resource, never()).close();
  }

  @Test
  public void pureIOBracketUseError() throws Exception {
    Kind<PureIO<Void, Throwable, ?>, String> bracket =
        PureIOMonadDefer.bracket(PureIO.<Void, Throwable, AutoCloseable>pure(resource),
                              r -> PureIO.<Void, Throwable, String>raiseError(new UnsupportedOperationException()));

    Either<Throwable, String> result = bracket.<PureIO<Void, Throwable, String>>fix().provide(null);

    assertTrue(result.isLeft());
    verify(resource).close();
  }
}
