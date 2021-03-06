package jp.co.cyberagent.aeromock.template.velocity

import java.io.{StringWriter, Writer}

import jp.co.cyberagent.aeromock.config.Project
import jp.co.cyberagent.aeromock.core.annotation.TemplateIdentifier
import jp.co.cyberagent.aeromock.core.http.ParsedRequest
import jp.co.cyberagent.aeromock.data.InstanceProjection
import jp.co.cyberagent.aeromock.helper._
import jp.co.cyberagent.aeromock.template.{TemplateAssertError, TemplateAssertFailure, TemplateAssertResult, TemplateService}
import jp.co.cyberagent.aeromock.{AeromockTemplateNotFoundException, AeromockTemplateParseException}
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import org.apache.velocity.exception.{ParseErrorException, ResourceNotFoundException}
import scaldi.Injector

import scala.language.dynamics

/**
 * [[jp.co.cyberagent.aeromock.template.TemplateService]] for Velocity.
 * @author stormcat24
 */
@TemplateIdentifier(name = "velocity", configType = classOf[Void], specialConfig = true)
class VelocityTemplateService(implicit val inj: Injector) extends TemplateService {

  VelocityConfigurationInitializer.initialize(project)

  /**
   * @inheritdoc
   */
  override def renderHtml(request: ParsedRequest, projection: InstanceProjection): String = {

    val templatePath = request.url + extension
    val template = try {
      Velocity.getTemplate(templatePath)
    } catch {
      case e: ResourceNotFoundException => throw new AeromockTemplateNotFoundException(request.url + extension, e)
      case e: ParseErrorException => throw new AeromockTemplateParseException(templatePath, e)
    }

    val proxyMap = projection.toInstanceJava().asInstanceOf[java.util.Map[_, _]]
    val context = new VelocityContext(proxyMap)

    val writer = new StringWriter
    template.merge(context, writer)
    writer.toString
  }

  override def templateAssertProcess(templatePath: String): Either[TemplateAssertResult, (Any, Writer) => Unit] = {
    val startTimeMills = System.currentTimeMillis()
    try {
      Right((param: Any, writer: Writer) => {
        Velocity.getTemplate(templatePath).merge(new VelocityContext(param.asInstanceOf[java.util.Map[_, _]]), writer)
      })
    } catch {
      case e: ParseErrorException => Left(TemplateAssertFailure(getDifferenceSecondsFromNow(startTimeMills), e.getMessage))
      case e: Exception => Left(TemplateAssertError(getDifferenceSecondsFromNow(startTimeMills), e.getMessage))
    }

  }

  /**
   * @inheritdoc
   */
  override def extension: String = ".vm"
}
