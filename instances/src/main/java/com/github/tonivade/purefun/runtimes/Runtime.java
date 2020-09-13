/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.runtimes;

import static com.github.tonivade.purefun.concurrent.FutureOf.toFuture;
import static com.github.tonivade.purefun.effect.EIOOf.toEIO;
import static com.github.tonivade.purefun.effect.RIOOf.toRIO;
import static com.github.tonivade.purefun.effect.TaskOf.toTask;
import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
import static com.github.tonivade.purefun.effect.URIOOf.toURIO;
import static com.github.tonivade.purefun.effect.ZIOOf.toZIO;
import static com.github.tonivade.purefun.instances.FutureInstances.async;
import static com.github.tonivade.purefun.monad.IOOf.toIO;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.RIO_;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.URIO_;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.monad.IO_;

public interface Runtime<F extends Witness> {
  
  <T> T run(Kind<F, T> value);
  
  <T> Future<T> parRun(Kind<F, T> value, Executor executor);
  
  default <T> Future<T> parRun(Kind<F, T> value) {
    return parRun(value, Future.DEFAULT_EXECUTOR);
  }
  
  static Runtime<IO_> io() {
    return new Runtime<IO_>() {
      @Override
      public <T> T run(Kind<IO_, T> value) {
        return value.fix(toIO()).unsafeRunSync();
      }
      
      @Override
      public <T> Future<T> parRun(Kind<IO_, T> value, Executor executor) {
        return value.fix(toIO()).foldMap(async()).fix(toFuture());
      }
    };
  }
  
  static Runtime<UIO_> uio() {
    return new Runtime<UIO_>() {
      @Override
      public <T> T run(Kind<UIO_, T> value) {
        return value.fix(toUIO()).unsafeRunSync();
      }
      
      @Override
      public <T> Future<T> parRun(Kind<UIO_, T> value, Executor executor) {
        return value.fix(toUIO()).foldMap(async()).fix(toFuture());
      }
    };
  }
  
  static Runtime<Task_> task() {
    return new Runtime<Task_>() {
      @Override
      public <T> T run(Kind<Task_, T> value) {
        return value.fix(toTask()).safeRunSync().getOrElseThrow();
      }
      
      @Override
      public <T> Future<T> parRun(Kind<Task_, T> value, Executor executor) {
        return value.fix(toTask()).foldMap(async()).fix(toFuture());
      }
    };
  }
  
  static <R> Runtime<Kind<URIO_, R>> urio(R env) {
    return new Runtime<Kind<URIO_, R>>() {
      @Override
      public <T> T run(Kind<Kind<URIO_, R>, T> value) {
        return value.fix(toURIO()).unsafeRunSync(env);
      }
      
      @Override
      public <T> Future<T> parRun(Kind<Kind<URIO_, R>, T> value, Executor executor) {
        return value.fix(toURIO()).foldMap(env, async()).fix(toFuture());
      }
    };
  }
  
  static <R> Runtime<Kind<RIO_, R>> rio(R env) {
    return new Runtime<Kind<RIO_, R>>() {
      @Override
      public <T> T run(Kind<Kind<RIO_, R>, T> value) {
        return value.fix(toRIO()).safeRunSync(env).getOrElseThrow();
      }
      
      @Override
      public <T> Future<T> parRun(Kind<Kind<RIO_, R>, T> value, Executor executor) {
        return value.fix(toRIO()).foldMap(env, async()).fix(toFuture());
      }
    };
  }
  
  static <E> Runtime<Kind<EIO_, E>> eio() {
    return new Runtime<Kind<EIO_, E>>() {
      @Override
      public <T> T run(Kind<Kind<EIO_, E>, T> value) {
        return value.fix(toEIO()).safeRunSync().getRight();
      }
      
      @Override
      public <T> Future<T> parRun(Kind<Kind<EIO_, E>, T> value, Executor executor) {
        return value.fix(toEIO()).foldMap(async()).fix(toFuture());
      }
    };
  }
  
  static <R, E> Runtime<Kind<Kind<ZIO_, R>, E>> zio(R env) {
    return new Runtime<Kind<Kind<ZIO_, R>, E>>() {
      @Override
      public <T> T run(Kind<Kind<Kind<ZIO_, R>, E>, T> value) {
        return value.fix(toZIO()).provide(env).getRight();
      }
      
      @Override
      public <T> Future<T> parRun(Kind<Kind<Kind<ZIO_, R>, E>, T> value, Executor executor) {
        return value.fix(toZIO()).foldMap(env, async()).fix(toFuture());
      }
    };
  }
}
