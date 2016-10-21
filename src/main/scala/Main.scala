import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.immutable._
import scala.concurrent.duration.Duration

/**
  * Created by Sebastian on 21.10.16.
  */
object Main extends App {

  implicit val as = ActorSystem()
  implicit val ex = as.dispatcher
  implicit val am = ActorMaterializer()

  def getData(implicit ec: ExecutionContext): Future[Seq[String]] = Future(Seq("abc", "de", "fghi"))

  val source = Source.fromFuture(getData).mapConcat(identity)

  /** ****************
    * Flows          *
    * ****************/
  val countChars: Flow[String, LetterCount, Unit] =
    Flow[String]
      .map { item =>
        (item, item.toCharArray.length)
      }

  val printChars: Flow[LetterCount, LetterCount, Unit] =
    Flow[LetterCount].map { item =>
      println(s"${item._1} : ${item._2}")
      item
    }

  val sumChars: Flow[LetterCount, Int, Unit] =
    Flow[LetterCount].fold(0) { (sum, item) =>
      sum + item._2
    }

  type LetterCount = (String, Int)

  /** ****************
    * Pipeline          *
    * ****************/
  val result: Future[Int] =
    source
      .via(countChars)
      .via(printChars)
      .via(sumChars)
      .runWith(Sink.head)

  result.map{ charCount =>
    println(s"sum chars: $charCount")
    as.shutdown()
  }
}
