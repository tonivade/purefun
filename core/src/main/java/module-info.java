module com.github.tonivade.purefun.core {
  exports com.github.tonivade.purefun.data;
  exports com.github.tonivade.purefun.concurrent;
  exports com.github.tonivade.purefun.core;
  exports com.github.tonivade.purefun.type;

  requires transitive com.github.tonivade.purefun;
  requires transitive java.compiler;
  requires transitive org.pcollections;
}