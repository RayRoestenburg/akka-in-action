package aia.stream.integration

import java.io.{BufferedReader, File, InputStreamReader, PrintWriter}
import java.net.Socket

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.{AmqpConnectionUri, AmqpSinkSettings, NamedQueueSourceSettings}
import akka.stream.scaladsl.{Flow, Framing, Keep, RunnableGraph, Sink, Source, Tcp}
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig, PredefinedVersion}
import com.rabbitmq.client.{AMQP, ConnectionFactory}
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ConsumerTest extends TestKit(ActorSystem("ConsumerTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers {

  implicit val materializer = ActorMaterializer()
  import Orders._

  val dir = new File("messages")

  val rabbitMq = {
    val config = new EmbeddedRabbitMqConfig.Builder()
      .version(PredefinedVersion.V3_6_9)
      .port(8899)
      .rabbitMqServerInitializationTimeoutInMillis(5000)
      .build
    val mq = new EmbeddedRabbitMq(config)
    mq.start()
    mq
  }

  override def beforeAll(): Unit = {
    if (!dir.exists()) {
      dir.mkdir()
    }
  }

  override def afterAll(): Unit = {
    system.terminate()
    rabbitMq.stop()
    FileUtils.deleteDirectory(dir)
  }

  "Consumer" must {
    "pickup xml files" in {

      val consumer: RunnableGraph[Future[Order]] =
        FileXmlOrderSource.watch(dir.toPath)
          .toMat(Sink.head[Order])(Keep.right)

      val consumedOrder: Future[Order] = consumer.run()

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>
      val msgFile = new File(dir, "msg1.xml")

      FileUtils.write(msgFile, xml.toString())

      Await.result(consumedOrder, 10.seconds) must be(msg)
    }
    "confirm xml TCPConnection" in {
      import Tcp._
      implicit val executionContext = system.dispatcher

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>

      val xmlStr = xml.toString.replace("\n", "")

      val tcpSource: Source[IncomingConnection, Future[ServerBinding]] =
        Tcp().bind("localhost", 8887)

      val (serverBindingFuture, orderProbeFuture) =
        tcpSource.map { connection =>

          val confirm = Source.single("<confirm>OK</confirm>\n")

          val handleFlow =
            Flow[ByteString]
              .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true))
              .map(_.utf8String)
              .via(parseOrderXmlFlow)
              .alsoToMat(TestSink.probe[Order])(Keep.right)
              .merge(confirm)
              .map(c => ByteString(c.toString))

          connection.handleWith(handleFlow)
        }.toMat(Sink.head)(Keep.both).run()

      val responseFuture = serverBindingFuture.map { _ =>
        // サーバーサイドのソケットがバインドされた後、
        // クライアントサイドのソケットを作成し、リクエストを送信
        val socket = new Socket("localhost", 8887)

        val outputWriter = new PrintWriter(socket.getOutputStream)
        val responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream))

        outputWriter.println(xmlStr)
        outputWriter.flush()
        val response = responseReader.readLine()
        responseReader.close()
        outputWriter.close()

        response
      }

      Await.result(orderProbeFuture, 10.seconds).requestNext() must be(msg)
      Await.result(responseFuture, 20.seconds) must be("<confirm>OK</confirm>")
    }
    "pickup xml AMQP" in {
      val queueName = "xmlTest"
      val amqpSourceSettings =
        NamedQueueSourceSettings(
          AmqpConnectionUri("amqp://localhost:8899"),
          queueName
        )

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>

      sendMQMessage(queueName, xml.toString)

      val consumer: RunnableGraph[Future[Order]] =
        AmqpXmlOrderSource(amqpSourceSettings)
          .toMat(Sink.head)(Keep.right)

      val consumedOrder: Future[Order] = consumer.run()
      Await.result(consumedOrder, 10 seconds) must be(msg)
    }
  }

  "The Producer" must {
    "send msg using AMQP" in {
      implicit val executionContext = system.dispatcher

      val queueName = "xmlTest"

      val amqpSinkSettings =
        AmqpSinkSettings(
          AmqpConnectionUri("amqp://localhost:8899"),
          routingKey = Some(queueName)
        )

      val msg = new Order("me", "Akka in Action", 10)

      val producer: RunnableGraph[NotUsed] =
        Source.single(msg)
          .to(AmqpXmlOrderSink(amqpSinkSettings))

      val amqpSourceSettings =
        NamedQueueSourceSettings(
          AmqpConnectionUri("amqp://localhost:8899"),
          queueName
        )

      val consumer: RunnableGraph[Future[Order]] =
        AmqpXmlOrderSource(amqpSourceSettings)
          .toMat(Sink.head)(Keep.right)

      producer.run()

      val consumedOrder: Future[Order] = consumer.run()
      Await.result(consumedOrder, 10 seconds) must be(msg)
    }
  }
  def sendMQMessage(queueName: String, msg: String): Unit = {

    // Create a ConnectionFactory
    val connectionFactory = new ConnectionFactory
    connectionFactory.setUri("amqp://localhost:8899")

    // Create a Connection
    val connection = connectionFactory.newConnection()

    // Create a Channel
    val channel = connection.createChannel()
    channel.queueDeclare(queueName, true, false, false, null)

    // send the message
    channel.basicPublish("", queueName,
      new AMQP.BasicProperties.Builder().build(),
      msg.getBytes()
    )

    // Clean up
    channel.close()
    connection.close()
  }
}
