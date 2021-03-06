package jp.co.cyberagent.aeromock.core.script

import java.nio.file.Path

import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import jp.co.cyberagent.aeromock.helper.trye
import jp.co.cyberagent.aeromock.{AeromockScriptBadReturnTypeException, AeromockScriptExecutionException}

import scala.reflect.ClassTag

/**
 * Directory based roovy script runner.
 * @author stormcat24
 */
class GroovyDirectiveScriptRunner(directory: Path) {

  val engine = new GroovyScriptEngine(directory.toAbsolutePath().toString())

  def run[ReturnType](scriptName: String, binding: Binding)(implicit classTag: ClassTag[ReturnType]): ReturnType = {
    require(scriptName != null)
    require(binding != null)

    val returnClass = implicitly[ClassTag[ReturnType]].runtimeClass

    (trye(engine.run(scriptName, binding)) {e => throw new AeromockScriptExecutionException(directory.resolve(scriptName), e)} match {
      case Left(e) => throw e
      case Right(v) => trye(returnClass.cast(v).asInstanceOf[ReturnType]) { e =>
        throw new AeromockScriptBadReturnTypeException(returnClass, directory.resolve(scriptName), e)
      }
    }).right.get
  }
}
