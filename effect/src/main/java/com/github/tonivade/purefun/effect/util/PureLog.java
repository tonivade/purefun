package com.github.tonivade.purefun.effect.util;

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.effect.RIO;

public interface PureLog {

  <R extends PureLog> PureLog.Service<R> logger(StackFrame frame);

  static <R extends PureLog> RIO<R, Unit> debug(Producer<String> message) {
    var frame = getFrame();
    return RIO.accessM(env -> env.<R>logger(frame).debug(message));
  }

  static <R extends PureLog> RIO<R, Unit> info(Producer<String> message) {
    var frame = getFrame();
    return RIO.accessM(env -> env.<R>logger(frame).info(message));
  }

  static <R extends PureLog> RIO<R, Unit> warn(Producer<String> message) {
    var frame = getFrame();
    return RIO.accessM(env -> env.<R>logger(frame).warn(message));
  }

  static <R extends PureLog> RIO<R,Unit> error(Producer<String> message) {
    var frame = getFrame();
    return RIO.accessM(env -> env.<R>logger(frame).error(message));
  }

  interface Service<R extends PureLog> {

    default RIO<R, Unit> debug(Producer<String> message) {
      return log(Level.FINE, message);
    }

    default RIO<R, Unit> info(Producer<String> message) {
      return log(Level.INFO, message);
    }

    default RIO<R, Unit> warn(Producer<String> message) {
      return log(Level.WARNING, message);
    }

    default RIO<R, Unit> error(Producer<String> message) {
      return log(Level.SEVERE, message);
    }

    RIO<R, Unit> log(Level level, Producer<String> message);
  }

  static PureLog javaUtilLogging() {
    return new PureLog() {
      @Override
      public <R extends PureLog> Service<R> logger(StackFrame frame) {
        return (level, message) -> {
          var logger = Logger.getLogger(frame.getClassName());
          return RIO.exec(() -> logger.logp(level, frame.getClassName(), frame.getMethodName(), message.get()));
        };
      }
    };
  }

  static PureLog test(final Queue<LogRecord> traces) {
    return new PureLog() {
      @Override
      public <R extends PureLog> Service<R> logger(StackFrame frame) {
        return (level, message) -> RIO.exec(() -> {
          var log = new LogRecord(level, message.get());
          log.setSourceClassName(frame.getClassName());
          log.setSourceMethodName(frame.getMethodName());
          traces.add(log);
        });
      }
    };
  }

  private static StackFrame getFrame() {
    var walker = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
    return walker.walk(frames -> frames.skip(2).findFirst().orElseThrow());
  }
}
