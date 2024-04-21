package com.github.tonivade.purefun.effect.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

class PureLogTest {

  @Test
  void test() {
    Queue<LogRecord> traces = new LinkedList<>();

    PureLog.info(() -> "this is a test").safeRunSync(PureLog.test(traces));

    LogRecord logRecord = traces.poll();
    assertEquals("this is a test", logRecord.getMessage());
    assertEquals(Level.INFO, logRecord.getLevel());
    assertEquals(PureLogTest.class.getName(), logRecord.getSourceClassName());
    assertEquals("test", logRecord.getSourceMethodName());
  }
}
