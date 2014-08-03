package aia.integration

import akka.testkit.TestKit
import akka.actor.{ Props, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import java.io._
import java.net.URL
import xml.XML
import akka.util.Timeout
import concurrent.duration._

class SprayRestTest extends TestKit(ActorSystem("SprayRestTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers {
  implicit val timeout: Timeout = 10 seconds
  implicit val executor = system.dispatcher

  val orderSystem = system.actorOf(Props[ProcessOrders])
  var orderHttp = new OrderHttpServer("localhost", 8182, orderSystem)

  override def beforeAll() {
    orderHttp.main(Array())
    Thread.sleep(5000)
  }

  override def afterAll() {
    orderHttp.stop()
    system.stop(orderSystem)
    Thread.sleep(5000)
    system.shutdown
  }

  "RestConsumer" must {

    "response when create" in {
      orderSystem ! "reset"
      val xml = <order>
                  <customerId>me</customerId>
                  <productId>Akka in Action</productId>
                  <number>10</number>
                </order>

      val urlConnection = new URL("http://localhost:8182/orderTest") ///orderTest")
      val conn = urlConnection.openConnection()
      conn.setDoOutput(true)
      conn.setRequestProperty(
        "Content-type",
        "text/xml; charset=UTF-8")

      val writer = new OutputStreamWriter(conn.getOutputStream)
      writer.write(xml.toString())
      writer.flush()

      val reader = new BufferedReader(
        new InputStreamReader((conn.getInputStream)))
      val response = new StringBuffer()
      var line = reader.readLine()
      while (line != null) {
        response.append(line)
        line = reader.readLine()
      }
      writer.close()
      reader.close()

      conn.getHeaderField(null) must be("HTTP/1.1 200 OK")

      val responseXml = XML.loadString(response.toString)
      val confirm = responseXml \\ "confirm"
      (confirm \\ "id").text must be("1")
      (confirm \\ "status").text must be("received")

    }
    "response when request status" in {
      //<start id="ch08-rest-spray-test-status"/>
      orderSystem ! "reset"
      val url = "http://localhost:8182/orderTest"
      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>

      val urlConnection = new URL(url)
      val conn = urlConnection.openConnection()
      conn.setDoOutput(true)
      conn.setRequestProperty("Content-type",
        "text/xml; charset=UTF-8")

      val writer = new OutputStreamWriter(conn.getOutputStream) //<co id="ch08-rest-spray-test-status-1"/>
      writer.write(xml.toString())
      writer.flush()
      //check result
      val reader = new BufferedReader(
        new InputStreamReader((conn.getInputStream)))
      val response = new StringBuffer()
      var line = reader.readLine()
      while (line != null) {
        response.append(line)
        line = reader.readLine()
      }
      writer.close()
      reader.close()
      conn.getHeaderField(null) must be("HTTP/1.1 200 OK") //<co id="ch08-rest-spray-test-status-2"/>

      val responseXml = XML.loadString(response.toString) //<co id="ch08-rest-spray-test-status-3"/>
      val confirm = responseXml \\ "confirm"
      (confirm \\ "id").text must be("1")
      (confirm \\ "status").text must be("received")

      val url2 = "http://localhost:8182/orderTest?id=1"
      val urlConnection2 = new URL(url2) //<co id="ch08-rest-spray-test-status-4"/>
      val conn2 = urlConnection2.openConnection()

      //Get response
      val reader2 = new BufferedReader(
        new InputStreamReader((conn2.getInputStream)))
      val response2 = new StringBuffer()
      line = reader2.readLine()
      while (line != null) {
        response2.append(line)
        line = reader2.readLine()
      }
      reader2.close()
      //check response
      conn2.getHeaderField(null) must be("HTTP/1.1 200 OK") //<co id="ch08-rest-spray-test-status-5"/>

      val responseXml2 = XML.loadString(response2.toString)
      val status = responseXml2 \\ "statusResponse" //<co id="ch08-rest-spray-test-status-6"/>
      (status \\ "id").text must be("1")
      (status \\ "status").text must be("processing")
      //<end id="ch08-rest-spray-test-status"/>
    }
    "response when missing id" in {
      orderSystem ! "reset"
      val url = "http://localhost:8182/orderTest"

      val urlConnection = new URL(url)
      val conn = urlConnection.openConnection()

      //check result
      val ex = evaluating {
        conn.getInputStream
      } must produce[FileNotFoundException]
      ex.getMessage must be("http://localhost:8182/orderTest")
    }
    "response when parsing error" in {
      orderSystem ! "reset"

      val url = "http://localhost:8182/orderTest"
      val xml = """<order><customerId>customer1</customerId>""" +
        """<productId>Akka in action</productId>"""

      val urlConnection = new URL(url)
      val conn = urlConnection.openConnection()
      conn.setDoOutput(true)
      conn.setRequestProperty("Content-type",
        "text/xml; charset=UTF-8");

      val writer = new OutputStreamWriter(conn.getOutputStream)
      writer.write(xml)
      writer.flush()
      //check result
      val ex = evaluating {
        conn.getInputStream
      } must produce[IOException]
      ex.getMessage must be(
        "Server returned HTTP response code: 500 for URL: " +
          "http://localhost:8182/orderTest")

      writer.close()

    }
  }
}
