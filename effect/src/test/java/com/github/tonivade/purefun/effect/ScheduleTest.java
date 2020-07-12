package com.github.tonivade.purefun.effect;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Tuple2;

class ScheduleTest {

  @Test
  void test() {
    Schedule<Nothing, Integer, Object, Integer> schedule = Schedule.repeat(3);
  
    Schedule<Nothing, Tuple2<Integer, Integer>, Object, Integer> fold = schedule.fold(0,
        (Tuple2<Integer, Integer> tuple) -> tuple.applyTo(Integer::sum));
  }

}
