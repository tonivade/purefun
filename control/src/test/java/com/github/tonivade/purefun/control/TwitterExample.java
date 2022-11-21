/*
 * Copyright (c) 2018-2022, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import static com.github.tonivade.purefun.control.ControlOf.toControl;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.typeclasses.Instances;

class TwitterExample {
  
  record Tweet(String userId, String message) {}
  
  interface Twitter {
    Control<ImmutableList<Tweet>> userTweets(String usuerId);
  }
  
  static final class TwitterImpl<R> implements Handler<R, Twitter>, Twitter {
    
    @Override
    public Twitter effect() { return this; }

    @Override
    public Control<ImmutableList<Tweet>> userTweets(String userId) {
      return use(resume -> resume.apply(listOf(new Tweet(userId, "asdfg"))));
    }
  }
    
  static <R> Control<R> instanceOf(Function1<Twitter, Control<R>> apply) {
    return new TwitterImpl<R>().apply(apply);
  }
  
  private Control<ImmutableList<Tweet>> program(Twitter twitter) {
    return Instances.<Control_>monad().use()
      .then(twitter.userTweets("12345"))
      .then(twitter.userTweets("54321"))
      .apply(ImmutableList::appendAll)
      .fix(toControl());
  }
  
  @Test
  void runProgram( ){
    assertEquals(listOf(new Tweet("12345", "asdfg"), new Tweet("54321", "asdfg")), instanceOf(this::program).run());
  }

}
