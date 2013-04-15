package uk.co.sam_giles.finagled_batch;

import com.twitter.finagle.{Http, Service}
import com.twitter.util.{Await, Future}
import java.net.{InetSocketAddress, URL}
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.buffer.{ChannelBufferInputStream, ChannelBufferOutputStream, ChannelBuffer, ChannelBuffers}
import com.fasterxml.jackson.databind.{ Module, ObjectMapper }
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.nio.charset.Charset
import java.nio.ByteBuffer
import scala.util.parsing.combinator._
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._
import com.twitter.finagle.service.TimeoutFilter._
import com.twitter.finagle.service.TimeoutFilter
import com.twitter.util.Duration
import com.twitter.finagle.GlobalRequestTimeoutException
import org.jboss.netty.util.Timer
import com.twitter.finagle.util.DefaultTimer
import java.util.concurrent.TimeUnit
import com.twitter.finagle.IndividualRequestTimeoutException



case class Request(url: URL, method: String, headers: Map[String, String], body: String)
case class Response(status: Int, headers: Map[String, String], body: String)

/**
 * Defines an instance of a Server.
 */
object Server {

  val jsonObjectMapper =  { 
    val mapper = new ObjectMapper 
    mapper.registerModule(DefaultScalaModule)
  }
  
  def getRequests(req: HttpRequest): Seq[Request] = {
      val content = req.getContent

      val inStream = new ChannelBufferInputStream(content)

      jsonObjectMapper.readValue(inStream, classOf[Array[Request]])
  }
  
  /**
   * Create an HttpReq HttpResp service.  This is a equivalent to (HttpRequest => Future[HttpResponse])
   */
  val service = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest): Future[HttpResponse] = {
      val profileStart = System.nanoTime.toDouble

      val timeout = req.getHeader("request-timeout") match {
        case null => 500
        case timeout => timeout.toInt
      }
      
      val batchService = new BatchService({ req: Request => 
        
        val port = req.url.getPort match {
          case n if n < 1 => ":" + 80
          case _ => ":" + _
        }
        
    	val clientService: Service[HttpRequest, HttpResponse] = Http.newService(req.url.getHost + port)
    	
    	val timeoutFilter = new HttpTimeoutFilter(Duration(timeout, TimeUnit.SECONDS))
    	val client = timeoutFilter andThen clientService
    	
    	
    	val httpRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(req.method), req.url.toURI.toString)
    	
    	req.headers foreach { header => 
    	  httpRequest.setHeader(header._1, header._2)
    	}
    	
    	client(httpRequest)
      })
      
      val batchedResponse = batchService(getRequests(req))

      val result = batchedResponse onSuccess {
          responses: Seq[HttpResponse] => {
            println("Got a seq of responses: " + responses.length)
          }
      } apply // Blocks until all complete. TODO: Should probably have a timeout option
      
      // Map a Seq[HttpResponse] from requests to a Seq[Response] for better JSON serialisation.
      val collectedResponses = result map { resp =>

        	  // Get the headers.
              val headers = resp.getHeaderNames.asScala flatMap { name => 
                
                println("Mapping: " + name)
                scala.collection.mutable.Seq(name -> resp.getHeader(name))
              }

              // Get the charset if possible using the Content-Type parser.  This is a massive hack but Parsers are cool  Check HTTP spec for default handling
              val charset = resp.containsHeader("Content-Type") match {
                case x if x => ContentTypeHeaderParser.parse(resp.getHeader("Content-Type")).param._2
                case _ => "UTF-8"
              }

              new Response(resp.getStatus.getCode, headers.toMap, resp.getContent.toString(Charset.forName(charset)))
            }
      
      val json = jsonObjectMapper.writeValueAsBytes(collectedResponses)
      val actualResponse: HttpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
      actualResponse.setContent(ChannelBuffers.copiedBuffer(json))
      actualResponse.setHeader("Content-Length", json.length)
      actualResponse.setHeader("X-Batch-Count", collectedResponses.length)
      val profileEnd = System.nanoTime.toDouble
      
      actualResponse.setHeader("X-Batch-Took", ((profileEnd - profileStart) / 1000000000).toString + "s")
      // Set the Futures value.
      Future.value(actualResponse)
    }
  }

  def start = {
    val server = Http.serve(":8081", service)
    Await.ready(server)
  }
}

class HttpTimeoutFilter(timeout: Duration)
  extends TimeoutFilter[HttpRequest, HttpResponse](timeout,
    new IndividualRequestTimeoutException(timeout), DefaultTimer.twitter) {
}

case class ContentType(contentType: String, param: (String, String))

object ContentTypeHeaderParser extends JavaTokenParsers {

  def contentType: Parser[ContentType] = charsetEntry

  def charsetEntry: Parser[ContentType] = (
		  mime ~ opt(charsetParam) ^^ {
		    case mime ~ Some(cset) => {
                      val charset = cset.split('=');

                      if (charset.length == 2) {
                        ContentType(mime, (charset(0), charset(1)))
                      } else {
                        ContentType(mime, ("charset", "default"))
                      }
                    }
                    case mime ~ None => ContentType(mime, ("charset", "default"))
		  }
  )

  def mime = """[-\w\+]+/[-\w\+]+""".r

  val charset = """[a-zA-Z0-9\-]+""".r // matches a-z A-Z 0-9 and a - in a token.


  val charsetParam = ";" ~> "charset" ~ "=" ~> charset

  val contenttype = mime ~> charsetParam

  def parse(input: String): ContentType = parse(contentType, input).get
}
