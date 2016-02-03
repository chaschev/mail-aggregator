package com.chaschev.mail

import java.util.concurrent.ConcurrentHashMap
import javax.mail.Store

import com.chaschev.mail.MailApp.GlobalContext
import org.joda.time.DateTime

import scala.collection.concurrent.Map

import scala.collection.convert.decorateAsScala._

/**
  * Created by andrey on 2/3/16.
  */
case class ActiveStore(server: MailServer, store: Store, startedAt: DateTime = new DateTime())

object ConnectionManager {
  val maxPerBox: Int = GlobalContext.conf.global.connectionLimitPerServer

  private var stores: Map[String, ActiveStore] = new ConcurrentHashMap[String, ActiveStore]().asScala


}
