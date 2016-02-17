package com.chaschev.mail.conf

/**
  * Created by andrey on 2/17/16.
  */
case class MailboxDescs(descs: List[MailboxDesc])

case class MailboxDesc(name: String, aliases: List[String])
