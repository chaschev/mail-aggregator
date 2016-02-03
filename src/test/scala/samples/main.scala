package samples

import com.chaschev.mail.{CacheManager, AppOptions}
import com.chaschev.mail.MailApp.GlobalContext

/**
  * Created by andrey on 2/3/16.
  */
object main {
  def main(args: Array[String]) {
    val options = new AppOptions(args)

    GlobalContext.conf.mailServers.size

    if(options.has(AppOptions.FETCH_MODE)){
      CacheManager.getFile()
    } else
    if(options.has(AppOptions.PRINT_GRAPH_MODE)){

    } else {
      options.printHelpOn(100, 40)
    }
  }
}
