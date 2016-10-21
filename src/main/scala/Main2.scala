import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Request
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.{Await, Future}

object Main2 extends App {

  /** *********************************************
    * helper                                      *
    * *********************************************/
  def currThread = Thread.currentThread().getId

  def print(msg: String) = println(s"Thread($currThread at ${System.currentTimeMillis()}) : $msg")

  /** *********************************************
    * init                                        *
    * *********************************************/
  implicit val as = ActorSystem()
  implicit val ex = as.dispatcher
  implicit val am = ActorMaterializer()

  /** *********************************************
    * actor that produces input                   *
    * *********************************************/
  class ActorSource extends ActorPublisher[Int] {

    var buffer: List[Int] = (0 until 1000).toList

    def onRequest(requestedAmount: Long) = {
      print(s"requested amount: $requestedAmount, total demant: $totalDemand")
      print(s"buffer before demand: ${buffer.size}")

      val (take, keep) = buffer.splitAt(requestedAmount.toInt)
      buffer = keep
      take.foreach { i =>
        onNext(i) //send out the data
      }

      print(s"buffer after demand: ${buffer.size}")

      if (buffer.isEmpty) {
        print("Stream is finished")
        onCompleteThenStop()
      }
    }

    //the api
    override def receive: Receive = {
      case Request(requestedAmount: Long) => onRequest(requestedAmount)
      case a: Any => print(s"got msg: ${a.getClass}")
    }
  }

  /** *********************************************
    * print parallel                              *
    * *********************************************/
  val parallelismFactor = 5
  val printParallel: Flow[Int, Int, Unit] =
    Flow[Int].mapAsync(parallelismFactor) { item =>
      Future {
        if (item >= 40 && item <= 60) Thread.sleep(1000)
        print(s"$item")
        item
      }
    }

  /** *********************************************
    * create the source                           *
    * *********************************************/
  val actor = Props(new ActorSource())
  val source = Source.actorPublisher[Int](actor)

  /** *********************************************
    * run the pipeline                            *
    * *********************************************/
  val result =
    source
      .via(printParallel)
      .runWith(Sink.ignore)

  /** *********************************************
    * shutdown                                    *
    * *********************************************/
  result.foreach { result =>
    print("finished")
    as.shutdown()
  }

}
