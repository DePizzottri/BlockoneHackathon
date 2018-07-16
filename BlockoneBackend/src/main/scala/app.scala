import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import scala.io.StdIn

import java.math.BigInteger
import com.micronautics.web3j.{Cmd, Ether, EthereumSynchronous, Web3JScala}
import Cmd.{isMac, isWindows}
import org.web3j.protocol.Web3j
import org.web3j.protocol.ipc.{UnixIpcService, WindowsIpcService}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Promise}
import org.web3j.protocol.core.DefaultBlockParameterName._
import org.web3j.protocol.core._
import org.web3j.protocol.core.methods.response._
import com.micronautics.web3j.TransactionHash

import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.JavaConverters._
import scala.collection.mutable._

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.Imports._

object BlockoneBackend {
  var web3j: Web3j = null
  var web3jScala: Web3JScala = null

  var mongoClient =  com.mongodb.casbah.MongoClient("localhost", 27017)
  var db = mongoClient("ethereum")
  var col = db("txs")

  def main(args: Array[String]) {
    if(args.size < 2) {
      println("========================")
      println("usage args <mongo host address> <port>")
      println("========================")
      return ()
    }

    val mongoAddr = args(0)
    val port = args(1).toInt

    println(s"MongoDB host: $mongoAddr")
    mongoClient =  com.mongodb.casbah.MongoClient(mongoAddr, 27017)
    db = mongoClient("ethereum")
    col = db("txs")

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    import akka.http.scaladsl.model.headers._

    val cors = List(RawHeader("Access-Control-Allow-Origin", "*"), RawHeader("Access-Control-Allow-Headers", "*"))

    val eth = BigDecimal("1000000000000000000")

    val calcFee = (txs:scala.collection.immutable.Map[String, DBObject]) => {
      for(tx <- txs) {
        val gasUsed = BigInt(tx._2.getAsOrElse("gasUsed", "0"))
        val gasPrice = BigInt(tx._2.getAsOrElse("gasPrice", "0"))

        tx._2 += ("fee" -> (BigDecimal(gasUsed*gasPrice) / eth).toString)
      }
    }

    val isGoodTx = (tx:DBObject) => {
      val gas = BigInt(tx.getAsOrElse("gas", "0"))
      val gasUsed = BigInt(tx.getAsOrElse("gasUsed", "0"))

      if(gasUsed < gas) {
        true
      } else {
        if(gasUsed == gas && gas == BigInt("21000"))
          true
        else
          false
      }
    }

    val fixAmount = (txs:scala.collection.immutable.Map[String, DBObject]) => {
      for(tx <- txs) {
	val amount = BigDecimal(tx._2.getAsOrElse[String]("amount", "0"))
               
	tx._2 -= "amount"
        tx._2 += ("amount" -> (amount / eth).toString)
      }      
    }

    val route =
      get {
        respondWithDefaultHeaders(cors) {
          path("txs") {
            parameters("addr".as[String]){ addr =>
              val from = col.find(MongoDBObject("from" -> addr.toLowerCase)).map{x => (x.getAsOrElse[String]("hash", ""), x)}.toMap
              val to = col.find(MongoDBObject("to" -> addr.toLowerCase)).map{x => (x.getAsOrElse[String]("hash", ""), x)}.toMap

              calcFee(from)
              calcFee(to)

              fixAmount(from)
              fixAmount(to)

              val ret = "[" +
                from.values.mkString(",") +
                (if(to.size != 0) "," else "") +
                to.values.mkString(",") +
                "]"

              complete(StatusCodes.OK, ret)
            }
          } ~
          path("balance") {
            parameters("addr".as[String]){ addr =>
              val from = col.find(MongoDBObject("from" -> addr.toLowerCase)).map{x => (x.getAsOrElse[String]("hash", ""), x)}.toMap
              val to = col.find(MongoDBObject("to" -> addr.toLowerCase)).map{x => (x.getAsOrElse[String]("hash", ""), x)}.toMap

              calcFee(from)
              calcFee(to)

              var balance = BigDecimal("0")

              for(tx <- to if isGoodTx(tx._2)) {
                balance = balance + BigDecimal(tx._2.getAsOrElse("amount", "0"))
              }

	      //println(s"Positive balance $balance")

              for(tx <- from) {
                val fee = BigDecimal(tx._2.getAsOrElse("fee", "0"))

                balance = balance - BigDecimal(tx._2.getAsOrElse("amount", "0"))
                balance = balance - fee
              }

	      println(s"Result balance $balance")

              val bal = balance / eth

              complete(s"""{"balance": ${bal.toString}}""")
            }
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", port)

    println(s"Server is online\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
