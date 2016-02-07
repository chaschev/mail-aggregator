package com.chaschev.mail

import java.io.{FileOutputStream, PrintStream}

import com.chaschev.mail.AppOptions.{FETCH_MODE, FORCE_FETCH, PRINT_GRAPH_MODE}
import com.chaschev.mail.MailApp.{FetchMode, GlobalContext}
import com.chaschev.mail.graph.Graph
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.Duration

import scala.collection.mutable
import scala.util.control.NonFatal

/**
  * Created by andrey on 2/3/16.
  */
object main {
  val logger: Logger = LogManager.getLogger(getClass)

  def main(args: Array[String]) {
    AppOptions.init

    logger.info("parsing command line")
    val options = new AppOptions(args)

    import GlobalContext.cacheManager

    if(options.has(FETCH_MODE)){
      GlobalContext.fetchMode = Some(new FetchMode())
      for(mailServer <- GlobalContext.conf.mailServers) {
        val fetchMode = GlobalContext.fetchMode.get

        fetchMode.supplierServerPool.submit(new Runnable {
          override def run(): Unit = {
            for(mailbox <- mailServer.mailboxes) {
              try {
                val updateInterval = Duration.standardHours(GlobalContext.conf.global.updateIntervalHours)
                val updatedRecently = mailbox.updatedInLast(updateInterval)
                val forceFetch = options.has(FORCE_FETCH)

                val updateNeeded = forceFetch || !updatedRecently

                if(updateNeeded) {
                  logger.info(s"updating messages for $mailbox, updatedRecently: $updatedRecently, forceFetch: $forceFetch")

                  val connection = fetchMode.connectionManager.getConnection(mailServer)

                  var storeOpt: Option[ActiveStore] = None

                  while(!Thread.interrupted() && storeOpt.isEmpty) {
                    storeOpt = connection.tryAcquire(mailbox)

                    storeOpt match {
                      case Some(store) => try {
                        fetchMode.connectionManager.updateMessages(mailServer, mailbox, store)

                        mailServer.updateStats()
                      } finally {
                        connection.release(mailbox, storeOpt.get)
                      }
                      case None =>
                    }

                  }

                }
              }
              catch {
                case NonFatal(e) =>
                  logger.warn("exception in thread", e)
              }
              finally {
                cacheManager.unload(mailbox)
              }
            }

          }
        })

        fetchMode.supplierServerPool.shutdown()

      }
    } else
    if(options.has(PRINT_GRAPH_MODE)){
      GlobalContext.cacheManager.init()

      val graph = Graph()

      GlobalContext.iterateOverMessages((srv, mailbox, mailboxCached, folder, message) => {
        graph.addAll(message.fromEmails(), message.toEmails())
      })

      val nodesCSVOut = new PrintStream(new FileOutputStream("graph-nodes.csv"))

      nodesCSVOut.println("Id,Label")
      for(node <- graph.list) {
        nodesCSVOut.println(s"${node.num},${node.name}")
      }

      nodesCSVOut.close()

      val edgesCSVOut = new PrintStream(new FileOutputStream("graph-edges.csv"))

      edgesCSVOut.println("Source,Target,Type,Id,Label,Interval,Weight")

      var i = 0
      for(node1 <- graph.list) {
        val neighborsOpt = graph.graph.get(node1.name)
        neighborsOpt match {
          case Some(neighborsSet) =>
            for(name2 <- neighborsSet) {
              val node2 = graph.stringToNode(name2)

              edgesCSVOut.println(s"${node1.num},${node2.num},Undirected,$i,,,1.0")
              i += 1
            }
          case None =>
            logger.warn(s"didn't find ${node1.name}")
        }

      }
      edgesCSVOut.close()

    } else {
      println(options.printHelpOn(100, 10))
    }
  }
}
