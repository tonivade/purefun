module com.github.tonivade.purefun.instances {
  exports com.github.tonivade.purefun.runtimes;
  exports com.github.tonivade.purefun.instances;

  requires transitive com.github.tonivade.purefun;
  requires transitive com.github.tonivade.purefun.core;
  requires transitive com.github.tonivade.purefun.effect;
  requires transitive com.github.tonivade.purefun.free;
  requires transitive com.github.tonivade.purefun.monad;
  requires transitive com.github.tonivade.purefun.stream;
  requires transitive com.github.tonivade.purefun.transformer;
  requires transitive com.github.tonivade.purefun.typeclasses;
  requires transitive com.github.tonivade.purefun.control;
}