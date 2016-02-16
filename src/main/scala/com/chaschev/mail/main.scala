package com.chaschev.mail

import java.io.{FileOutputStream, PrintStream}
import java.util.concurrent.TimeUnit

import com.chaschev.mail.AppOptions.{FETCH_MODE, FORCE_FETCH, PRINT_GRAPH_MODE}
import com.chaschev.mail.MailApp.{FetchMode, GlobalContext}
import com.chaschev.mail.graph.{GraphNode, Graph}
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.Duration

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
            logger.info(s"found ${mailServer.mailboxes.length} mailboxes: ${mailServer.mailboxes}")

            for(mailbox <- mailServer.mailboxes) {
              try {
                val updateInterval = Duration.standardHours(GlobalContext.conf.global.updateIntervalHours)
                val updatedRecently = mailbox.updatedInLast(updateInterval)
                val forceFetch = options.has(FORCE_FETCH)

                val updateNeeded = forceFetch || !updatedRecently

                logger.info(s"$mailbox - updatedRecently: $updatedRecently, forceFetch: $forceFetch")

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
                        connection.release(mailbox, store)
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

        fetchMode.supplierServerPool.awaitTermination(1, TimeUnit.HOURS)
        fetchMode.supplierServerPool.shutdown()

      }
    } else
    if(options.has(PRINT_GRAPH_MODE)){
      GlobalContext.cacheManager.init()

      val graph = Graph()

      GlobalContext.iterateOverMessages((srv, mailbox, mailboxCached, folder, message) => {
        graph.addAllAll(message.fromEmails(), message.toEmails())
      })

      val nodesCSVOut = new PrintStream(new FileOutputStream("graph-nodes.csv"))

      nodesCSVOut.println("Id,Label,Interval")
      for(node <- graph.list) {
        nodesCSVOut.println(s"${node.num},${node.num} ${node.name},")
      }

      nodesCSVOut.close()

      val edgesCSVOut = new PrintStream(new FileOutputStream("graph-edges.csv"))

      edgesCSVOut.println("Source,Target,Type")

      val nodesCount = graph.list.length
      var edgesCount = 0

      for(node1 <- graph.list) {
        val nodes2  = graph.getNondirectSiblings(node1)

        if(graph.isIsolated(node1)) {
          logger.warn(s"$node1 is isolated")
        }

        for(node2 <- nodes2) {
          edgesCSVOut.println(s"${node1.num},${node2.num},Undirected")
          edgesCount += 1
        }
      }

      edgesCSVOut.close()

      println(s"done. nodes: $nodesCount, edges: $edgesCount")

    } else {
      println(options.printHelpOn(100, 10))
    }
  }
}
