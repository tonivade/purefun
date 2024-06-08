module com.github.tonivade.purefun.instances {
  exports com.github.tonivade.purefun.runtimes;
  exports com.github.tonivade.purefun.instances;

  requires transitive com.github.tonivade.purefun;
  requires transitive com.github.tonivade.purefun.core;
  requires static com.github.tonivade.purefun.effect;
  requires static com.github.tonivade.purefun.free;
  requires static com.github.tonivade.purefun.monad;
  requires static com.github.tonivade.purefun.stream;
  requires static com.github.tonivade.purefun.transformer;
  requires static com.github.tonivade.purefun.typeclasses;
}