package aia.integration

import akka.testkit.{ ImplicitSender, TestProbe, TestKit }
import akka.actor.{ Props, ActorSystem }
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, MustMatchers}
import akka.camel.{CamelMessage, CamelExtension}
import concurrent.{ Future, ExecutionContext, Await }
import scala.concurrent.duration._
import java.io.{ InputStreamReader, BufferedReader, PrintWriter, File }
import org.apache.commons.io.FileUtils
import java.net.{ InetSocketAddress, SocketAddress, ServerSocket, Socket }
import org.apache.activemq.camel.component.ActiveMQComponent
import javax.jms.{ Session, DeliveryMode, Connection }
import org.apache.activemq.ActiveMQConnectionFactory
import org.apache.activemq.broker.BrokerRegistry
import collection.JavaConversions._

class ConsumerTest extends TestKit(ActorSystem("ConsumerTest"))
  with WordSpecLike with BeforeAndAfterAll with MustMatchers
  with ImplicitSender {

  val dir = new File("messages")

  override def beforeAll() {
    if (!dir.exists()) {
      dir.mkdir()
    }
    //remove active mq data if it exists
    val mqData = new File("activemq-data")
    if(mqData.exists())
      FileUtils.deleteDirectory(mqData)
  }

  override def afterAll() {
    system.terminate()
    FileUtils.deleteDirectory(dir)
  }

  "Consumer" must {
    "pickup xml files" in {
      //<start id="ch08-order-consumer-test-create"/>
      val probe = TestProbe()
      val camelUri = "file:messages"
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri, probe.ref)))
      //<end id="ch08-order-consumer-test-create"/>

      //<start id="ch08-order-consumer-test-wait"/>
      val camelExtention = CamelExtension(system) //<co id="ch08-order-consumer-test-0"/>
      val activated = camelExtention.activationFutureFor( //<co id="ch08-order-consumer-test-1"/>
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      Await.ready(activated, 5 seconds) //<co id="ch08-order-consumer-test-2"/>
      //<end id="ch08-order-consumer-test-wait"/>
      //<start id="ch08-order-consumer-test"/>
      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order> //<co id="ch08-order-consumer-test-3"/>
      val msgFile = new File(dir, "msg1.xml")

      FileUtils.write(msgFile, xml.toString()) //<co id="ch08-order-consumer-test-4"/>

      probe.expectMsg(msg) //<co id="ch08-order-consumer-test-5"/>

      system.stop(consumer)
      //<end id="ch08-order-consumer-test"/>
    }
    "pickup xml TCPConnection" in {
      //<start id="ch08-order-consumer-test-tcp"/>
      val probe = TestProbe()
      val camelUri =
        "mina:tcp://localhost:8888?textline=true&sync=false" //<co id="ch08-order-consumer-test-tcp-1"/>
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri, probe.ref)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      Await.ready(activated, 5 seconds)

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>

      val xmlStr = xml.toString().replace("\n", "") //<co id="ch08-order-consumer-test-tcp-2"/>
      val sock = new Socket("localhost", 8888)
      val ouputWriter = new PrintWriter(sock.getOutputStream, true)
      ouputWriter.println(xmlStr) //<co id="ch08-order-consumer-test-tcp-3"/>
      ouputWriter.flush()

      probe.expectMsg(msg)

      ouputWriter.close()
      system.stop(consumer)
      //<end id="ch08-order-consumer-test-tcp"/>
    }
    "confirm xml TCPConnection" in {
      //<start id="ch08-order-consumer-test-confirm"/>
      val probe = TestProbe()
      val camelUri =
        "mina:tcp://localhost:8887?textline=true" //<co id="ch08-order-consumer-test-confirm-1"/>
      val consumer = system.actorOf(
        Props(new OrderConfirmConsumerXml(camelUri, probe.ref)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      Await.ready(activated, 5 seconds)

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>

      val xmlStr = xml.toString().replace("\n", "")
      val sock = new Socket("localhost", 8887)
      val ouputWriter = new PrintWriter(sock.getOutputStream, true)
      ouputWriter.println(xmlStr)
      ouputWriter.flush()
      val responseReader = new BufferedReader(
        new InputStreamReader(sock.getInputStream))
      val response = responseReader.readLine() //<co id="ch08-order-consumer-test-confirm-2"/>
      response must be("<confirm>OK</confirm>")
      probe.expectMsg(msg) //<co id="ch08-order-consumer-test-confirm-3"/>

      responseReader.close()
      ouputWriter.close()
      system.stop(consumer)
      //<end id="ch08-order-consumer-test-confirm"/>
    }
    "pickup xml ActiveMQ" in {
      val probe = TestProbe()

      //<start id="ch08-order-consumer-test-mq-add"/>
      val camelContext = CamelExtension(system).context
      camelContext.addComponent("activemq", //<co id="ch08-order-consumer-test-mq-1"/>
        ActiveMQComponent.activeMQComponent(
          "vm:(broker:(tcp://localhost:8899)?persistent=false)"))
      //<end id="ch08-order-consumer-test-mq-add"/>

      //<start id="ch08-order-consumer-test-mq"/>
      val camelUri = "activemq:queue:xmlTest" //<co id="ch08-order-consumer-test-mq-2"/>
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri, probe.ref)))

      val activated = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      Await.ready(activated, 5 seconds)

      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>

      sendMQMessage(xml.toString())
      probe.expectMsg(msg)

      system.stop(consumer)
      //<end id="ch08-order-consumer-test-mq"/>
      //<start id="ch08-order-consumer-test-mq-broker"/>
      val brokers = BrokerRegistry.getInstance().getBrokers
      brokers.foreach { case (name, broker) => broker.stop() }
      //<end id="ch08-order-consumer-test-mq-broker"/>
    }
    "pickup 100 xml files" in {
      val probe = TestProbe()
      val camelUri = "file:messages"
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri, probe.ref)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      Await.ready(activated, 5 seconds)

      for (nr <- 1 until 100) {
        val msg = new Order("me", "Akka in Action", nr)
        val xml = <order>
                    <customerId>{ msg.customerId }</customerId>
                    <productId>{ msg.productId }</productId>
                    <number>{ msg.number }</number>
                  </order>
        val msgFile = new File(dir, "msg%d.xml".format(nr))
        FileUtils.write(msgFile, xml.toString())

        probe.expectMsg(msg)
      }
      system.stop(consumer)
    }
    "pickup 100 xml TCPConnection" in {
      val probe = TestProbe()
      val camelUri =
        "mina:tcp://localhost:8886?textline=true&sync=false"
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri, probe.ref)))
      val activated = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      Await.ready(activated, 5 seconds)

      val sock = new Socket("localhost", 8886)
      val ouputWriter = new PrintWriter(sock.getOutputStream, true)

      for (nr <- 1 until 100) {
        val msg = new Order("me", "Akka in Action", nr)
        val xml = <order>
                    <customerId>{ msg.customerId }</customerId>
                    <productId>{ msg.productId }</productId>
                    <number>{ msg.number }</number>
                  </order>

        val xmlStr = xml.toString().replace("\n", "")
        ouputWriter.println(xmlStr)
        ouputWriter.flush()

        probe.expectMsg(msg)
      }
      ouputWriter.close()
      system.stop(consumer)
    }

  }

  "The Producer" must {
    "send msg using TCPConnection" in {
      //<start id="ch08-order-producer-test-simple-start"/>
      implicit val ExecutionContext = system.dispatcher
      val probe = TestProbe()
      val camelUri = "mina:tcp://localhost:8885?textline=true"
      val consumer = system.actorOf(
        Props(new OrderConfirmConsumerXml(camelUri, probe.ref)))

      val producer = system.actorOf(
        Props(new SimpleProducer(camelUri))) //<co id="ch08-order-producer-test-1"/>
      val activatedCons = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      val activatedProd = CamelExtension(system).activationFutureFor(
        producer)(timeout = 10 seconds, executor = system.dispatcher)
      val camel = Future.sequence(List(activatedCons, activatedProd)) //<co id="ch08-order-producer-test-2"/>
      Await.result(camel, 5 seconds)
      //<end id="ch08-order-producer-test-simple-start"/>
      //<start id="ch08-order-producer-test-simple"/>
      val msg = new Order("me", "Akka in Action", 10)
      val xml = <order>
                  <customerId>{ msg.customerId }</customerId>
                  <productId>{ msg.productId }</productId>
                  <number>{ msg.number }</number>
                </order>

      val xmlStr = xml.toString().replace("\n", "")
      val probeSend = TestProbe()
      probeSend.send(producer,xmlStr) //<co id="ch08-order-producer-test-3"/>

      probe.expectMsg(msg) //<co id="ch08-order-producer-test-4"/>

      val recvMsg = probeSend.expectMsgType[CamelMessage](3.seconds)
      recvMsg.body.asInstanceOf[String] must be ("<confirm>OK</confirm>")

      system.stop(producer)
      system.stop(consumer)
      //<end id="ch08-order-producer-test-simple"/>

      val deac = CamelExtension(system).deactivationFutureFor(consumer)(timeout = 10 seconds, executor = system.dispatcher)
      Await.result(deac, 5 seconds)
    }
    "send Xml using TCPConnection" in {
      implicit val ExecutionContext = system.dispatcher
      val probe = TestProbe()
      val camelUri =
        "mina:tcp://localhost:8884?textline=true&sync=false"
      val consumer = system.actorOf(
        Props(new OrderConsumerXml(camelUri, probe.ref)))

      val camelProducerUri = "mina:tcp://localhost:8884?textline=true"
      val producer = system.actorOf(
        Props(new OrderProducerXml(camelProducerUri)))
      val activatedCons = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      val activatedProd = CamelExtension(system).activationFutureFor(
        producer)(timeout = 10 seconds, executor = system.dispatcher)
      val camel = Future.sequence(List(activatedCons, activatedProd))
      Await.result(camel, 5 seconds)

      val msg = new Order("me", "Akka in Action", 10)
      producer ! msg

      probe.expectMsg(msg)

      system.stop(producer)
      system.stop(consumer)
    }
    "receive confirmation when send Xml" in {
      //<start id="ch08-order-producer-test-confirm"/>
      implicit val ExecutionContext = system.dispatcher
      val probe = TestProbe()
      val camelUri ="mina:tcp://localhost:9889?textline=true"
      val consumer = system.actorOf(
        Props(new OrderConfirmConsumerXml(camelUri, probe.ref)))

      val producer = system.actorOf(
        Props(new OrderConfirmProducerXml(camelUri)))

      val activatedCons = CamelExtension(system).activationFutureFor(
        consumer)(timeout = 10 seconds, executor = system.dispatcher)
      val activatedProd = CamelExtension(system).activationFutureFor(
        producer)(timeout = 10 seconds, executor = system.dispatcher)

      val camel = Future.sequence(List(activatedCons, activatedProd))
      Await.result(camel, 5 seconds)
      val probeSend = TestProbe()
      val msg = new Order("me", "Akka in Action", 10)
      probeSend.send(producer, msg)
      probe.expectMsg(msg) //<co id="ch08-order-producer3-test-1"/>
      probeSend.expectMsg("OK") //<co id="ch08-order-producer3-test-2"/>

      system.stop(producer)
      system.stop(consumer)
      //<end id="ch08-order-producer-test-confirm"/>
    }
  }
  def sendMQMessage(msg: String) {
    // Create a ConnectionFactory
    val connectionFactory =
      new ActiveMQConnectionFactory("tcp://localhost:8899");

    // Create a Connection
    val connection: Connection = connectionFactory.createConnection()
    connection.start()

    // Create a Session
    val session = connection.createSession(false,
      Session.AUTO_ACKNOWLEDGE)

    // Create the destination (Topic or Queue)
    val destination = session.createQueue("xmlTest");

    // Create a MessageProducer from the Session to the Topic or Queue
    val producer = session.createProducer(destination);
    producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

    // Create a messages
    val message = session.createTextMessage(msg);

    // Tell the producer to send the message
    producer.send(message);

    // Clean up
    session.close();
    connection.close();
  }
}
