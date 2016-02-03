package com.chaschev.mail

/**
  * Created by andrey on 2/3/16.
  */
object MailStatus extends Enumeration {
  type MailStatus = Value
  val fetched, timeout, error = Value
}
