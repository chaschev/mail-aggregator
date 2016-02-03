package com.chaschev.mail

/**
  * Created by andrey on 2/2/16.
  */

case class GlobalConfiguration(
    threadCount: Int = 64,
    connectionLimitPerServer: Int = 8,
    timeoutSec: Int = 60,
    retries: Int = 5
    )

case class JsonConfiguration(
    global: GlobalConfiguration,
    mailServers: List[MailServer] = Nil
    ) {


}
