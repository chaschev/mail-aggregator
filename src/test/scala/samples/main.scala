package samples

import com.chaschev.mail.MailApp.GlobalContext
import com.chaschev.mail.{ActiveStore, AppOptions}
import org.apache.logging.log4j.{LogManager, Logger}
import org.joda.time.Duration

/**
  * Created by andrey on 2/3/16.
  */
object main {
  val logger: Logger = LogManager.getLogger(getClass)

  def main(args: Array[String]) {

    logger.info("parsing command line")
    val options = new AppOptions(args)

    import GlobalContext.cacheManager

    if(options.has(options.FETCH_MODE)){
      cacheManager.init()

      for(mailServer <- GlobalContext.conf.mailServers) {
        val fetchMode = GlobalContext.fetchMode.get
        fetchMode.supplierServerPool.submit(new Runnable {
          override def run(): Unit = {
            for(mailbox <- mailServer.mailboxes) {
              try {
                var updateInterval = Duration.parse(GlobalContext.conf.global.updateInterval)
                var updatedRecently = mailbox.updatedInLast(updateInterval)
                var forceFetch = options.has(options.FORCE_FETCH)

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
              } finally {
                cacheManager.unload(mailbox)
              }
            }

          }
        })


      }
    } else
    if(options.has(options.PRINT_GRAPH_MODE)){
      GlobalContext.cacheManager.init()

    } else {
      options.printHelpOn(100, 40)
    }
  }
}
