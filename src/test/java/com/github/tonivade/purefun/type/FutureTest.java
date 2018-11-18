package com.github.tonivade.purefun.type;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tonivade.purefun.Consumer1;

public class FutureTest {
  
  @Test
  public void onSuccess() throws InterruptedException {
    Consumer1<String> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.of(() -> {
      return "Hello World!";
    });

    Thread.sleep(1000);
    
    future.onSuccess(consumer1);
    
    verify(consumer1).accept("Hello World!");
  }
  
  @Test
  public void onSuccessTimeout() {
    Consumer1<String> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.of(() -> {
      Thread.sleep(1000);
      return "Hello World!";
    });
    
    future.onSuccess(consumer1);
    
    verify(consumer1, timeout(2000)).accept("Hello World!");
  }
  
  @Test
  public void onFailure() throws InterruptedException {
    Consumer1<Throwable> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.of(() -> {
      throw new RuntimeException();
    });

    Thread.sleep(1000);
    
    future.onFailure(consumer1);
    
    verify(consumer1).accept(any());
  }
  
  @Test
  public void onFailureTimeout() {
    Consumer1<Throwable> consumer1 = Mockito.mock(Consumer1.class);

    Future<String> future = Future.of(() -> {
      Thread.sleep(1000);
      throw new RuntimeException();
    });
    
    future.onFailure(consumer1);
    
    verify(consumer1, timeout(2000)).accept(any());
  }
}
