package com.manning.aa
package words

import java.net.URLEncoder

import scala.concurrent.duration._

import akka.actor._
import akka.cluster.routing.{ClusterRouterPoolSettings, ClusterRouterPool}
import akka.routing.BroadcastPool


object JobWorker {
  def props = Props[JobWorker]

  case class StartWork(jobName:String, master:ActorRef)
  case class Work(input:List[String], master:ActorRef)
  case object WorkLoadDepleted
  case object JobIsFinished
}

class JobWorker extends Actor
                   with ActorLogging {
  import JobMaster._
  import JobWorker._
  import context._

  var processed = 0

  def receive = idle
  
  def idle: Receive = {
    case StartWork(jobName, master) =>
      become(enlisted(jobName, master))

      log.info(s"Enlisted, will start requesting work for job '${jobName}'.")
      master ! EnlistWorker(self)
      master ! GiveMeMoreWork
      watch(master)

      setReceiveTimeout(30 seconds)
  }
  
  def enlisted(jobName:String, master:ActorRef): Receive = {
    case ReceiveTimeout =>
      master ! GiveMeMoreWork

    case Work(textPart, master) =>
      val countMap = textPart.flatMap(_.split("\\W+"))
                             .foldLeft(Map.empty[String, Int]) { (count, word) =>
                               if(word == "FAIL") throw new RuntimeException("SIMULATED FAILURE!")
                               count + (word -> (count.getOrElse(word, 0) + 1))
                             }
      processed = processed + 1
      master ! PartialCount(countMap)
      master ! GiveMeMoreWork

    case WorkLoadDepleted =>
      log.info(s"Work load ${jobName} is depleted, retiring...")
      become(retired(jobName, master))
      setReceiveTimeout(Duration.Undefined)

    case JobIsFinished =>
      log.info(s"Job is finished, processed $processed parts, stopping self.")
      stop(self)

    case Terminated(master) =>
      log.error(s"Master terminated that ran Job ${jobName}, stopping self.")
      stop(self)
  }
  
  def retired(jobName:String, master:ActorRef): Receive = {
    case JobIsFinished =>
      log.info(s"Job ${jobName} is finished, processed $processed parts, stopping self.")
      stop(self)

    case Terminated(master) =>
      log.error(s"Master terminated that ran Job ${jobName}, stopping self.")
      stop(self)
  }
}


object JobMaster {
  def props = Props[JobMaster]

  case class StartJob(name: String, text: List[String])
  case class EnlistWorker(worker:ActorRef)

  case object GiveMeMoreWork
  case class PartialCount(map: Map[String, Int])

  case object Start
  case object MergeCounts
}

trait BroadcastWork { this: Actor =>
  import JobWorker._

  def broadcastWork(jobName:String) = {
    val workerRouter = context.actorOf(
      ClusterRouterPool(BroadcastPool(10), ClusterRouterPoolSettings(
        totalInstances = 100, maxInstancesPerNode = 20,
        allowLocalRoutees = false, useRole = Some("worker"))).props(Props[JobWorker]),
      name = URLEncoder.encode(s"worker-router-$jobName", "UTF-8"))

    //TODO solve this.
    Thread.sleep(4000)
    workerRouter ! StartWork(jobName, self)
  }
}

class JobMaster extends Actor
                   with ActorLogging
                   with BroadcastWork {
  import JobReceptionist.WordCount
  import JobMaster._
  import JobWorker.{Work, WorkLoadDepleted, JobIsFinished}
  import context._

  var textParts = Vector[List[String]]()
  var intermediateResult = Vector[Map[String, Int]]()
  var workGiven = 0
  var workReceived = 0
  var workers = Set[ActorRef]()

  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = idle

  def idle: Receive = {
    case StartJob(name, text) =>
      become(preparing(name, text, sender))
      self ! Start
  }


  def preparing(jobName:String, text:List[String], respondTo:ActorRef): Receive = {
    case Start =>
      // TODO work this out.
      // use cluster router, and watch them.
      textParts = text.grouped(10).toVector
      broadcastWork(jobName)
      become(working(jobName, respondTo))
  }

  def working(jobName:String, receptionist:ActorRef): Receive = {
    case EnlistWorker(worker) =>
      watch(worker)
      workers  = workers + worker
    case GiveMeMoreWork =>
      if(textParts.isEmpty) {
        sender ! WorkLoadDepleted
      } else {
        sender ! Work(textParts.head, self)
        workGiven = workGiven + 1
        textParts = textParts.tail
      }

    case PartialCount(countMap) =>
      intermediateResult = intermediateResult :+ countMap
      workReceived = workReceived + 1

      if(textParts.isEmpty && workGiven == workReceived) {
        become(finishing(jobName, receptionist, workers))
        self ! MergeCounts
      }

    case Terminated(worker) =>
      log.info(s"Worker $worker got terminated. Cancelling job $jobName.")
      stop(self)
  }
  
  def finishing(jobName:String, receptionist:ActorRef, workers:Set[ActorRef]): Receive = {
    case MergeCounts =>
      val mergedMap = intermediateResult.foldLeft(Map[String, Int]()){ (el, acc) =>
        el.map { case (word, count) =>
          acc.get(word).map( accCount => (word ->  (accCount + count))).getOrElse(word -> count)
        } ++ (acc -- el.keys)
      }
      workers.foreach(_ ! JobIsFinished)
      receptionist ! WordCount(jobName, mergedMap)

    case Terminated(worker) =>
      log.info(s"Job $jobName is finishing. Worker ${worker.path.name} is stopped.")
  }
}


object JobReceptionist {
  def props = Props[JobReceptionist]
  def name = "receptionist"
  case class JobRequest(name: String, text: List[String])
  case class Job(name: String, text: List[String], respondTo: ActorRef, jobMaster:ActorRef)
  case class WordCount(name:String, map: Map[String, Int])

  sealed trait Response
  case class JobSuccess(name:String, map: Map[String, Int]) extends Response
  case class JobFailure(name:String) extends Response
}

trait CreateMaster {
  def context:ActorContext
  def createMaster(name:String) = context.actorOf(JobMaster.props, name)
}

class JobReceptionist extends Actor
                         with ActorLogging
                         with CreateMaster {
  import JobReceptionist._
  import JobMaster.StartJob
  import context._

  var jobs = Set[Job]()
  var retries = Map[String, Int]()
  val maxRetries = 10


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
            // TODO change this to send message to another receptionist in the cluster?
            // TODO change this to remove failing worker node?
            self.tell(JobRequest(name, text), failedJob.respondTo)
          } else self.tell(JobRequest(name, failedJob.text), failedJob.respondTo)

          retries = retries + retries.get(name).map(r=> name -> (r + 1)).getOrElse(name -> 1)
        }
      }
  }
}