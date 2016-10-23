package aia.integration

import akka.testkit.{ TestProbe, TestKit }
import akka.actor.{ Props, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import java.io._
import java.net.URL
import concurrent.Await
import akka.camel.CamelExtension
import scala.concurrent.duration._
import xml.XML
import akka.util.Timeout

class CamelRestTest extends TestKit(ActorSystem("CamelRestTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers {
  implicit val timeout: Timeout = 10 seconds
  implicit val executor = system.dispatcher

  override def afterAll(): Unit = {
    system.terminate()
  }

  "RestConsumer" must {

    "response when create" in {

      val orderSystem = system.actorOf(Props[ProcessOrders])
      val camelUri =
        "jetty:http://localhost:8181/orderTest"
      val consumer = system.actorOf(
        Props(new OrderConsumerRest(camelUri, orderSystem)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)
      Await.result(activated, 5 seconds)



      val xml = <order>
                  <customerId>me</customerId>
                  <productId>Akka in Action</productId>
                  <number>10</number>
                </order>

      val urlConnection = new URL("http://localhost:8181/orderTest")
      val conn = urlConnection.openConnection()
      conn.setDoOutput(true)
      conn.setRequestProperty(
        "Content-type",
        "text/xml; charset=UTF-8")

      val writer = new OutputStreamWriter(conn.getOutputStream)
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



      conn.getHeaderField(null) must be("HTTP/1.1 200 OK")

      val responseXml = XML.loadString(response.toString)
      val confirm = responseXml \\ "confirm"
      (confirm \\ "id").text must be("1")
      (confirm \\ "status").text must be("received")



      system.stop(consumer)
      system.stop(orderSystem)
      Await.result(
        CamelExtension(system).deactivationFutureFor(consumer),
        5 seconds)

    }
    "response when request status" in {
      val orderSystem = system.actorOf(Props[ProcessOrders])
      val camelUri =
        "jetty:http://localhost:8181/orderTest"
      val consumer = system.actorOf(
        Props(new OrderConsumerRest(camelUri, orderSystem)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)
      Await.result(activated, 5 seconds)

      val url = "http://localhost:8181/orderTest"
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
        "text/xml; charset=UTF-8");

      val writer = new OutputStreamWriter(conn.getOutputStream)
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
      conn.getHeaderField(null) must be("HTTP/1.1 200 OK")

      val responseXml = XML.loadString(response.toString)
      val confirm = responseXml \\ "confirm"
      (confirm \\ "id").text must be("1")
      (confirm \\ "status").text must be("received")


      val url2 = "http://localhost:8181/orderTest?id=1"
      val urlConnection2 = new URL(url2)
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

      conn2.getHeaderField(null) must be("HTTP/1.1 200 OK")

      val responseXml2 = XML.loadString(response2.toString)
      val status = responseXml2 \\ "statusResponse"
      (status \\ "id").text must be("1")
      (status \\ "status").text must be("processing")


      system.stop(consumer)
      system.stop(orderSystem)
      Await.result(
        CamelExtension(system).deactivationFutureFor(consumer),
        5 seconds)
    }
    "response when missing id" in {
      val probe = TestProbe()
      val camelUri =
        "jetty:http://localhost:8181/orderTest"
      val consumer = system.actorOf(
        Props(new OrderConsumerRest(camelUri, probe.ref)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)
      Await.result(activated, 5 seconds)

      val url = "http://localhost:8181/orderTest"

      val urlConnection = new URL(url)
      val conn = urlConnection.openConnection()

      //check result

      val ex = the [IOException] thrownBy {
        conn.getInputStream
      }
      ex.getMessage must be(
        "Server returned HTTP response code: 400 for URL: " +
          "http://localhost:8181/orderTest")

      system.stop(consumer)
      Await.result(
        CamelExtension(system).deactivationFutureFor(consumer),
        5 seconds)
    }
    "response when parsing error" in {
      val probe = TestProbe()
      val camelUri =
        "jetty:http://localhost:8181/orderTest"
      val consumer = system.actorOf(
        Props(new OrderConsumerRest(camelUri, probe.ref)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)
      Await.result(activated, 5 seconds)


      val url = "http://localhost:8181/orderTest"
      val xml = """<order><customerId>customer1</customerId>
      <productId>Akka in action</productId>"""

      val urlConnection = new URL(url)
      val conn = urlConnection.openConnection()
      conn.setDoOutput(true)
      conn.setRequestProperty("Content-type",
        "text/xml; charset=UTF-8");


      val writer = new OutputStreamWriter(conn.getOutputStream)
      writer.write(xml)
      writer.flush()
      //check result

      val ex = the [IOException] thrownBy  {
        conn.getInputStream
      }
      ex.getMessage must be(
        "Server returned HTTP response code: 500 for URL: " +
          "http://localhost:8181/orderTest")


      writer.close()
      system.stop(consumer)
      Await.result(
        CamelExtension(system).deactivationFutureFor(consumer),
        5 seconds)
    }
  }
}
