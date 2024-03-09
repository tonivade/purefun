/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.effect.PureIOOf.toPureIO;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIO_;
import com.github.tonivade.purefun.instances.EitherTInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.instances.OptionTInstances;
import com.github.tonivade.purefun.instances.PureIOInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.transformer.EitherT;
import com.github.tonivade.purefun.transformer.EitherTOf;
import com.github.tonivade.purefun.transformer.EitherT_;
import com.github.tonivade.purefun.transformer.OptionT;
import com.github.tonivade.purefun.transformer.OptionTOf;
import com.github.tonivade.purefun.transformer.OptionT_;
import com.github.tonivade.purefun.type.Either;

@ExtendWith(MockitoExtension.class)
public class MonadDeferTest {

  private MonadDefer<IO_> ioMonadDefer = IOInstances.monadDefer();
  private MonadDefer<Kind<Kind<PureIO_, Void>, Throwable>> PureIOMonadDefer =
      PureIOInstances.monadDefer();
  private MonadDefer<Kind<Kind<EitherT_, IO_>, Throwable>> eitherTMonadDefer =
      EitherTInstances.monadDefer(ioMonadDefer);
  private MonadDefer<Kind<OptionT_, IO_>> optionTMonadDefer =
      OptionTInstances.monadDefer(ioMonadDefer);

  private AutoCloseable resource = Mockito.mock(AutoCloseable.class);

  @Test
  public void ioLater(@Mock Producer<String> task) throws Exception {
    when(task.get()).thenReturn("hola toni");

    Kind<IO_, String> later = ioMonadDefer.later(task);

    String result = later.fix(toIO()).unsafeRunSync();

    assertEquals("hola toni", result);
    verify(task).get();
  }

  @Test
  public void ioBracket() throws Exception {
    Kind<IO_, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.pure("done"));

    String result = bracket.fix(toIO()).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void ioBracketAcquireError() throws Exception {
    Kind<IO_, String> bracket =
        ioMonadDefer.bracket(IO.<AutoCloseable>raiseError(new IllegalStateException()), r -> IO.pure("done"));

    assertThrows(IllegalStateException.class, () -> bracket.fix(toIO()).unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void ioBracketUseError() throws Exception {
    Kind<IO_, String> bracket =
        ioMonadDefer.bracket(IO.pure(resource), r -> IO.<String>raiseError(new UnsupportedOperationException()));

    assertThrows(UnsupportedOperationException.class, () -> bracket.fix(toIO()).unsafeRunSync());

    verify(resource).close();
  }

  @Test
  public void eitherTBracket() throws Exception {
    Kind<Kind<Kind<EitherT_, IO_>, Throwable>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.<IO_, Throwable, AutoCloseable>right(IOInstances.monad(), resource),
                                                r -> EitherT.<IO_, Throwable, String>right(IOInstances.monad(), "done"));

    String result = bracket.fix(EitherTOf::narrowK).get().fix(toIO()).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  public void eitherTBracketAcquireError() throws Exception {
    Kind<Kind<Kind<EitherT_, IO_>, Throwable>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.<IO_, Throwable, AutoCloseable>left(IOInstances.monad(), new IllegalStateException()),
                                                r -> EitherT.<IO_, Throwable, String>right(IOInstances.monad(), "done"));

    assertThrows(IllegalStateException.class,
                 () -> bracket.fix(EitherTOf::narrowK).value().fix(toIO()).unsafeRunSync());

    verify(resource, never()).close();
  }

  @Test
  public void eitherTBracketUseError() throws Exception {
    Kind<Kind<Kind<EitherT_, IO_>, Throwable>, String> bracket =
        eitherTMonadDefer.bracket(EitherT.<IO_, Throwable, AutoCloseable>right(IOInstances.monad(), resource),
                                                r -> EitherT.<IO_, Throwable, String>left(IOInstances.monad(), new UnsupportedOperationException()));

    Either<Throwable, String> unsafeRunSync = bracket.fix(EitherTOf::narrowK).value().fix(toIO()).unsafeRunSync();

    assertTrue(unsafeRunSync.getLeft() instanceof UnsupportedOperationException);
    verify(resource).close();
  }

  @Test
  public void optionTBracket() throws Exception {
    Kind<Kind<OptionT_, IO_>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource),
                                  r -> OptionT.some(IOInstances.monad(), "done"));

    String result = bracket.fix(OptionTOf::narrowK).getOrElseThrow().fix(toIO()).unsafeRunSync();

    assertEquals("done", result);
    verify(resource).close();
  }

  @Test
  @Disabled
  public void optionTBracketAcquireError() throws Exception {
    Kind<Kind<OptionT_, IO_>, String> bracket =
        optionTMonadDefer.bracket(OptionT.<IO_, AutoCloseable>none(IOInstances.monad()),
                                  r -> OptionT.some(IOInstances.monad(), "done"));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix(OptionTOf::narrowK).getOrElseThrow().fix(toIO()).unsafeRunSync());

    assertEquals("could not acquire resource", error.getMessage());
    verify(resource, never()).close();
  }

  @Test
  @Disabled
  public void optionTBracketUseError() throws Exception {
    Kind<Kind<OptionT_, IO_>, String> bracket =
        optionTMonadDefer.bracket(OptionT.some(IOInstances.monad(), resource),
                                  r -> OptionT.<IO_, String>none(IOInstances.monad()));

    NoSuchElementException error = assertThrows(NoSuchElementException.class,
                 () -> bracket.fix(OptionTOf::narrowK).getOrElseThrow().fix(toIO()).unsafeRunSync());

    assertEquals("get() in none", error.getMessage());
    verify(resource).close();
  }

  @Test
  public void PureIOBracket() throws Exception {
    Kind<Kind<Kind<PureIO_, Void>, Throwable>, String> bracket =
        PureIOMonadDefer.bracket(PureIO.<Void, Throwable, AutoCloseable>pure(resource),
                              r -> PureIO.<Void, Throwable, String>pure("done"));

    Either<Throwable, String> result = bracket.fix(toPureIO()).provide(null);

    assertEquals(Either.right("done"), result);
    verify(resource).close();
  }

  @Test
  public void PureIOBracketAcquireError() throws Exception {
    Kind<Kind<Kind<PureIO_, Void>, Throwable>, String> bracket =
        PureIOMonadDefer.bracket(PureIO.<Void, Throwable, AutoCloseable>raiseError(new IllegalStateException()),
                              r -> PureIO.<Void, Throwable, String>pure("done"));

    Either<Throwable, String> result = bracket.fix(toPureIO()).provide(null);

    assertTrue(result.isLeft());
    verify(resource, never()).close();
  }

  @Test
  public void PureIOBracketUseError() throws Exception {
    Kind<Kind<Kind<PureIO_, Void>, Throwable>, String> bracket =
        PureIOMonadDefer.bracket(PureIO.<Void, Throwable, AutoCloseable>pure(resource),
                              r -> PureIO.<Void, Throwable, String>raiseError(new UnsupportedOperationException()));

    Either<Throwable, String> result = bracket.fix(toPureIO()).provide(null);

    assertTrue(result.isLeft());
    verify(resource).close();
  }
}
