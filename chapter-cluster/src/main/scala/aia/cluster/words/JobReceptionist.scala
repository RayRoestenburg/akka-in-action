package aia.cluster
package words

import java.net.URLEncoder

import akka.actor._
import akka.actor.Terminated


object JobReceptionist {
  def props = Props(new JobReceptionist)
  def name = "receptionist"

  case class JobRequest(name: String, text: List[String])

  sealed trait Response
  case class JobSuccess(name: String, map: Map[String, Int]) extends Response
  case class JobFailure(name: String) extends Response

  case class WordCount(name: String, map: Map[String, Int])

  case class Job(name: String, text: List[String], respondTo: ActorRef, jobMaster: ActorRef)
}

class JobReceptionist extends Actor
                         with ActorLogging
                         with CreateMaster {
  import JobReceptionist._
  import JobMaster.StartJob
  import context._

  override def supervisorStrategy: SupervisorStrategy =
    SupervisorStrategy.stoppingStrategy

  var jobs = Set[Job]()
  var retries = Map[String, Int]()
  val maxRetries = 3


  def receive = {
    case jr @ JobRequest(name, text) =>
      log.info(s"Received job $name")

      val masterName = "master-"+URLEncoder.encode(name, "UTF8")
      val jobMaster = createMaster(masterName)

      val job = Job(name, text, sender, jobMaster)
      jobs = jobs + job

      jobMaster ! StartJob(name, text)
      watch(jobMaster)

    case WordCount(jobName, map) =>
      log.info(s"Job $jobName complete.")
      log.info(s"result:${map}")
      jobs.find(_.name == jobName).foreach { job =>
        job.respondTo ! JobSuccess(jobName, map)
        stop(job.jobMaster)
        jobs = jobs - job
      }

    case Terminated(jobMaster) =>
      jobs.find(_.jobMaster == jobMaster).foreach { failedJob =>
        log.error(s"Job Master $jobMaster terminated before finishing job.")

        val name = failedJob.name
        log.error(s"Job ${name} failed.")
        val nrOfRetries = retries.getOrElse(name, 0)

        if(maxRetries > nrOfRetries) {
          if(nrOfRetries == maxRetries -1) {
            // Simulating that the Job worker will work just before max retries
            val text = failedJob.text.filterNot(_.contains("FAIL"))
            self.tell(JobRequest(name, text), failedJob.respondTo)
          } else self.tell(JobRequest(name, failedJob.text), failedJob.respondTo)

          retries = retries + retries.get(name).map(r=> name -> (r + 1)).getOrElse(name -> 1)
        }
      }
  }
}

trait CreateMaster {
  def context: ActorContext
  def createMaster(name: String) = context.actorOf(JobMaster.props, name)
}
