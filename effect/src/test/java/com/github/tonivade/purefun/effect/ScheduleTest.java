package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.Unit.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Either;

@ExtendWith(MockitoExtension.class)
public class ScheduleTest {

  @Test
  public void repeat(@Mock Consumer1<String> console) {
    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    ZIO<Nothing, Throwable, Integer> repeat = print.repeat(Schedule.recurs(2));
    
    Either<Throwable, Integer> provide = repeat.provide(nothing());
    
    assertEquals(Either.right(2), provide);
    verify(console, times(3)).accept("hola");
  }
  
  @Test
  public void noRepeat(@Mock Consumer1<String> console) {
    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    ZIO<Nothing, Throwable, Unit> repeat = print.repeat(Schedule.never());
    
    Either<Throwable, Unit> provide = repeat.provide(nothing());
    
    assertEquals(Either.right(unit()), provide);
    verify(console).accept("hola");
  }

  @Test
  public void retry(@Mock Producer<String> console) {
    when(console.get()).thenThrow(RuntimeException.class).thenReturn("hola");

    ZIO<Nothing, Throwable, String> read = ZIO.task(console::get);
    ZIO<Nothing, Throwable, String> retry = read.retry(Schedule.recurs(1));
    
    Either<Throwable, String> provide = retry.provide(nothing());
    
    assertEquals(Either.right("hola"), provide);
    verify(console, times(2)).get();
  }
  
  @Test
  public void noRetry(@Mock Producer<String> console) {
    when(console.get()).thenThrow(RuntimeException.class).thenReturn("hola");
    
    ZIO<Nothing, Throwable, String> read = ZIO.task(console::get);
    ZIO<Nothing, Throwable, String> retry = read.retry(Schedule.never());
    
    Either<Throwable, String> provide = retry.provide(nothing());
    
    assertTrue(provide.isLeft());
  }
}
