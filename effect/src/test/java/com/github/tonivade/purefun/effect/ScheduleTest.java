package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.type.Either;

@ExtendWith(MockitoExtension.class)
public class ScheduleTest {

  @Test
  public void repeat(@Mock Consumer1<String> console) {
    ZIO<Nothing, Throwable, Unit> print = ZIO.exec(() -> console.accept("hola"));
    ZIO<Nothing, Throwable, Integer> repeat = print.repeat(Schedule.repeat(2));
    
    Either<Throwable, Integer> provide = repeat.provide(nothing());
    
    assertEquals(Either.right(2), provide);
    verify(console, times(3)).accept("hola");
  }
}
