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

  <R extends PureLog> PureLog.Service<R> logger();

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

  @SafeVarargs
  static <T> PureLog jul(T...reified) {
    var clazz = getClassOf(reified);
    return new PureLog() {
      @Override
      public <R extends PureLog> Service<R> logger() {
        return (level, message) -> {
          var frame = getFrame(clazz);
          var logger = Logger.getLogger(frame.getClassName());
          return RIO.exec(() -> logger.logp(level, frame.getClassName(), frame.getMethodName(), message.get()));
        };
      }
    };
  }

  @SafeVarargs
  static <T> PureLog test(final Queue<LogRecord> traces, T...reified) {
    var clazz = getClassOf(reified);
    return new PureLog() {
      @Override
      public <R extends PureLog> Service<R> logger() {
        return (level, message) -> {
          var frame = getFrame(clazz);
          return RIO.exec(() -> {
            LogRecord log = new LogRecord(level, message.get());
            log.setSourceClassName(frame.getClassName());
            log.setSourceMethodName(frame.getMethodName());
            traces.add(log);
          });
        };
      }
    };
  }

  private static StackFrame getFrame(Class<?> clazz) {
    var walker = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);
    return walker.walk(frames -> frames.filter(f -> f.getClassName().equals(clazz.getName())).findFirst().orElseThrow());
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> getClassOf(T... reified) {
    if (reified.length > 0) {
      throw new IllegalArgumentException("do not pass arguments to this function, it's just a trick to get refied types");
    }
    return (Class<T>) reified.getClass().getComponentType();
  }
}
