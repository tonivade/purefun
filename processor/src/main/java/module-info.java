import com.github.tonivade.purefun.processor.HigherKindProcessor;

module com.github.tonivade.purefun.processor {
  exports com.github.tonivade.purefun.processor;

  provides javax.annotation.processing.Processor with HigherKindProcessor;

  requires transitive com.github.tonivade.purefun;
  requires transitive java.compiler;
}