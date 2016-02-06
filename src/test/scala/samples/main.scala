package samples

import com.chaschev.mail.AppOptions.{PRINT_GRAPH_MODE, FORCE_FETCH, FETCH_MODE}
import com.chaschev.mail.MailApp.{FetchMode, GlobalContext}
import com.chaschev.mail.{ActiveStore, AppOptions}
import joptsimple.OptionParser
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.Duration
import scala.collection.convert.decorateAsJava._
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
                      } finally {
                        connection.release(mailbox, storeOpt.get)
                      }
                      case None =>
                    }

                    if(storeOpt.isDefined) {

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


      }
    } else
    if(options.has(PRINT_GRAPH_MODE)){
      GlobalContext.cacheManager.init()

    } else {
      println(options.printHelpOn(100, 10))
    }
  }
}
