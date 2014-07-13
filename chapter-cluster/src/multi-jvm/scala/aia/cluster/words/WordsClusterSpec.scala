package aia.cluster
package words

import scala.concurrent.duration._

import akka.actor.Props
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberUp}

import akka.testkit.ImplicitSender
import akka.remote.testkit.MultiNodeSpec
import JobReceptionist._


class WordsClusterSpecMultiJvmNode1 extends WordsClusterSpec
class WordsClusterSpecMultiJvmNode2 extends WordsClusterSpec
class WordsClusterSpecMultiJvmNode3 extends WordsClusterSpec
class WordsClusterSpecMultiJvmNode4 extends WordsClusterSpec

class WordsClusterSpec extends MultiNodeSpec(WordsClusterSpecConfig)
with STMultiNodeSpec with ImplicitSender {

  import WordsClusterSpecConfig._

  def initialParticipants = roles.size

  val seedAddress = node(seed).address
  val masterAddress = node(master).address
  val worker1Address = node(worker1).address
  val worker2Address = node(worker2).address

  muteDeadLetters(classOf[Any])(system)

  "A Words cluster" must {

    "form the cluster" in within(10 seconds) {

      Cluster(system).subscribe(testActor, classOf[MemberUp])
      expectMsgClass(classOf[CurrentClusterState])


      Cluster(system).join(seedAddress)

      receiveN(4).map { case MemberUp(m) => m.address }.toSet must be(
        Set(seedAddress, masterAddress, worker1Address, worker2Address))

      Cluster(system).unsubscribe(testActor)

      enterBarrier("cluster-up")
    }

    "execute a words job once the cluster is running" in within(10 seconds) {
      runOn(master) {
        val receptionist = system.actorOf(Props[JobReceptionist], "receptionist")
        receptionist ! JobRequest("job-1", List("some", "some very long text", "some long text"))
        expectMsg(JobSuccess("job-1", Map("some" -> 3, "very" -> 1, "long" -> 2, "text" -> 2)))
      }
      enterBarrier("job-done")
    }

    "continue to process a job when failures occur" in within(10 seconds) {
      runOn(master) {
        val receptionist = system.actorSelection("/user/receptionist")
        receptionist ! JobRequest("job-2", List("some", "FAIL", "some very long text", "some long text"))
        expectMsg(JobSuccess("job-2", Map("some" -> 3, "very" -> 1, "long" -> 2, "text" -> 2)))
      }
      enterBarrier("job-done")
    }
  }
}
