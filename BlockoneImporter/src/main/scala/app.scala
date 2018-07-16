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

object EthScanImporter {
  var web3j: Web3j = null
  var web3jScala: Web3JScala = null

  var mongoClient =  com.mongodb.casbah.MongoClient("localhost", 27017)
  var db = mongoClient("ethereum")
  var col = db("txs")

  def getTxsFromBlock(blockNum: Long): List[Transaction] = {
      val optBlock = web3jScala.sync.blockByNumber(new DefaultBlockParameterNumber(blockNum), true)
      if(optBlock.isEmpty)
        List.empty[Transaction]
      else
        optBlock.get.getTransactions.asScala.map{x => x.asInstanceOf[Transaction]}.toList
  }

  def isTxGood(tx:Transaction): Boolean = {
    val r = web3jScala.sync.transactionReceipt(new TransactionHash(tx.getHash.toString))

    if(r.isPresent) {
      BigInt(r.get.getGasUsed) < BigInt(tx.getGas)
    } else {
      false
    }
  }

  val database = HashMap[String, MutableList[BigInt]]()

  def processTx(tx:Transaction) {
      val r = web3jScala.sync.transactionReceipt(new TransactionHash(tx.getHash.toString))

      if(r.isPresent) {
        col.insert(MongoDBObject(
          "hash" -> tx.getHash,
          "from" -> tx.getFrom,
          "to" -> tx.getTo,
          "amount"-> tx.getValue.toString,
          "gas" -> tx.getGas.toString,
          "gasUsed" -> r.get.getGasUsed.toString,
          "gasPrice" -> tx.getGasPrice.toString
        ))
      } else {
        col.insert(MongoDBObject("from" -> tx.getFrom, "to" -> tx.getTo, "amount" -> tx.getValue.toString))
      }
  }

  def main(args: Array[String]) {
    if(args.size < 4) {
      println("========================")
      println("usage args <geth grpc address> <mongo host address> <begin block number> <end block number>")
      println("========================")
      return ()
    }

    val gethAddr = args(0)
    val mongoAddr = args(1)

    val beginBlock = args(2).toInt
    val endBlock = args(3).toInt

    web3j = Web3JScala.fromHttp(gethAddr)
    web3jScala = new Web3JScala(web3j)

    mongoClient = com.mongodb.casbah.MongoClient(mongoAddr, 27017)
    db = mongoClient("ethereum")
    col = db("txs")

    val web3ClientVersion1: String = web3jScala.sync.versionWeb3J

    println(s"Web3J version = $web3ClientVersion1")
    println(s"Last known block is ${web3jScala.sync.blockNumber}")

    println(s"Start import blocks from $beginBlock to $endBlock")

    for(bn <- beginBlock to endBlock) {
      val txs = getTxsFromBlock(bn)
      if(bn % 1000 == 0) {
        println(s"on $bn block")
      }
      for(tx <- txs) {
        //if(isTxGood(tx))
            processTx(tx)
      }
    }

  //   implicit val system = ActorSystem("my-system")
  //   implicit val materializer = ActorMaterializer()
  //   // needed for the future flatMap/onComplete in the end
  //   implicit val executionContext = system.dispatcher
  //
  //   val route =
  //     get {
  //       path("hello") {
  //         complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
  //       } ~
  //       path("getTxs") {
  //         parameters("addr".as[String]){ addr =>
  //           if(database.contains(addr))
  //             complete(s"Count of txs ${database.get(addr).size}")
  //           else
  //             complete("Address not found")
  //         }
  //       }
  //     }
  //
  //   val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
  //
  //   println(s"Server is online\nPress RETURN to stop...")
  //   StdIn.readLine() // let it run until user presses return
  //   bindingFuture
  //     .flatMap(_.unbind()) // trigger unbinding from the port
  //     .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
