module com.github.tonivade.purefun.core {
  exports com.github.tonivade.purefun.data;
  exports com.github.tonivade.purefun.concurrent;
  exports com.github.tonivade.purefun;
  exports com.github.tonivade.purefun.type;

  requires com.github.tonivade.purefun.annotation;
  requires java.compiler;
  requires org.pcollections;
}