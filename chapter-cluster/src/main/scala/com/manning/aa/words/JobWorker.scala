package com.manning.aa
package words

import java.net.URLEncoder

import akka.actor._
import akka.cluster.ClusterEvent._
import akka.routing._

import JobWorker._
import JobReceptionist._
import JobMaster._


object JobWorker {
  def props = Props[JobWorker]
  case class Work(name:String, input:List[String], master:ActorRef)
  case class StartWork(name:String, master:ActorRef)
  case object GiveMeMoreWork
  case class PartialCount(map: Map[String, Int])
}

// A Worker only does one job.
// once the job is finished it 'retires'.
class JobWorker extends Actor with ActorLogging {
  import JobWorker._
  import context._
  import scala.concurrent.duration._
  
  def receive = idle
  
  def idle: Receive = {
    case StartWork(name, master) =>
      become(enlisted(name, master))
      log.info(s"Enlisted, requesting work for job ${name} managed by ${master.path}.")
      master ! GiveMeMoreWork
      watch(master)
      setReceiveTimeout(30 seconds)
  }
  
  def enlisted(name:String, master:ActorRef): Receive = {
    case ReceiveTimeout =>
      master ! GiveMeMoreWork

    case Work(jobName, textPart, master) =>
      if(jobName == name) {
        log.info(s"Processing text for job ${name} managed by ${master.path}.")
        val countMap = textPart.flatMap(_.split("\\W+"))
          .foldLeft(Map.empty[String, Int]){
          (count, word) => count + (word -> (count.getOrElse(word, 0) + 1))
        }
        log.info(s"Sending task result back for job  ${name} managed by ${master.path}.")
        master ! PartialCount(countMap)
        master ! GiveMeMoreWork
      }

    case WorkLoadDepleted(job) =>
      if(name == job) {
        log.info(s"Work load ${name} is depleted, managed by ${master.path}, now retiring...")
        become(retired(name, master))
        setReceiveTimeout(Duration.Undefined)
      }

    case Terminated(master) =>
      log.info(s"Master terminated that ran Job ${name}, stopping self.")
      stop(self)
  }
  
  def retired(name:String, master:ActorRef): Receive = {
    case JobIsFinished(job) =>
      if(name == job) {
        log.info(s"Job ${name} is finished, managed by ${master.path}, stopping.")
        stop(self)
      }

    case Terminated(master) =>
      log.info(s"Master terminated that ran Job ${name}, stopping self.")
      stop(self)
  }
}


object JobMaster {
  def props = Props[JobMaster]
  case class WorkLoadDepleted(name: String)
  case class JobIsFinished(name: String)
  case object Start
  case object MergeCounts
}

// A job master is spawned, one per job.
class JobMaster extends Actor with ActorLogging {
  import JobMaster._
  import context._
  var textParts = Vector[List[String]]()
  var intermediateResult = Vector[Map[String, Int]]()
  var workGiven = 0
  var workReceived = 0

  def receive = idle

  def idle: Receive = {
    //
    case StartJob(name, text) =>
      become(preparing(name, text, sender))
      self ! Start
  }

  def preparing(jobName:String, text:List[String], respondTo:ActorRef): Receive = {
    case Start =>
      // group in texts of 10 lines each
      textParts = text.grouped(1).toVector
      val workers = (0 to 10).map(i=> context.actorOf(Props[JobWorker], "worker"+i))
      become(working(jobName, respondTo, workers.toList))
      workers.foreach(_ ! StartWork(jobName, self))


    case Terminated(worker) =>
      // create job workers
      // job workers
    case Routees(routees) =>

      routees foreach (r=> context.watch(r.asInstanceOf[ActorRefRoutee].ref))
  }

  def working(jobName:String, receptionist:ActorRef, workers:List[ActorRef]): Receive = {
    case GiveMeMoreWork =>
      if(textParts.isEmpty) sender ! WorkLoadDepleted(jobName)
      else {
        sender ! Work(jobName, textParts.head, self)
        workGiven = workGiven + 1
        textParts = textParts.tail
      }

    case PartialCount(countMap) =>
      log.info(s"received partial count $workReceived $workGiven $textParts")
      intermediateResult = intermediateResult :+ countMap
      workReceived = workReceived + 1

      if(textParts.isEmpty && workGiven == workReceived) {
        become(finishing(jobName, receptionist, workers))
        self ! MergeCounts
      }

  }
  
  def finishing(jobName:String, receptionist:ActorRef, workers:List[ActorRef]): Receive = {
    case MergeCounts =>
      log.info(s"merging $intermediateResult")
      val mergedMap = intermediateResult.foldLeft(Map[String, Int]()){ (el, acc) =>
        el.map { case (word, count) =>
          acc.get(word).map( accCount => (word ->  (accCount + count))).getOrElse(word -> count)
        } ++ (acc -- el.keys)
      }
      receptionist ! WordCount(jobName, mergedMap)
      workers.foreach(_ ! JobIsFinished(jobName))
    case Terminated(worker) => // this is expected

  }
}


object JobReceptionist {
  def props = Props[JobReceptionist]
  def name = "receptionist"
  case class StartJob(name: String, text: List[String])
  case class JobRequest(name: String, text: List[String])
  case class Job(name: String, text: List[String], respondTo: ActorRef, jobMaster:ActorRef)
  case class WordCount(name:String, map: Map[String, Int])
  case class JobResponse(name:String, map: Map[String, Int])
}

class JobReceptionist extends Actor {
  import JobReceptionist._
  import context._

  var jobs = Vector[Job]()

  def receive = {
    case jr @ JobRequest(name, text) =>
      val masterName = "master_"+URLEncoder.encode(name, "UTF8")
      val jobMaster = context.actorOf(JobMaster.props, masterName)

      val job = Job(name, text, sender, jobMaster)
      jobs = jobs :+ job

      jobMaster ! StartJob(name, text)
      watch(jobMaster)
    case WordCount(name, map) =>
      jobs.find(_.name == name).foreach(job => job.respondTo ! JobResponse(name, map))

    case Terminated(jobMaster) =>
      // job failed, so start over again
      jobs.find(_.jobMaster == jobMaster).foreach { failedJob =>
        // TODO start (somewhere else in the cluster?)
        self.tell(JobRequest(failedJob.name, failedJob.text), failedJob.respondTo)
      }
  }
}


object ClusterMonitor {

}

class ClusterMonitor extends Actor {
  def receive = {

    //TODO etc
    case MemberUp(member) =>
    case MemberExited(member) =>
    case MemberRemoved(member, previousStatus) =>

  }
}


//TODO JobMonitor. PubSub, subscribes to names of jobs, workers and masters talk about jobs.
//TODO Spray app using Cluster Client. handles request on HTTP
//TODO JobStore. Singleton.