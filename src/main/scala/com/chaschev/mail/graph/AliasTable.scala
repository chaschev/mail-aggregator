package com.chaschev.mail.graph

import com.chaschev.mail.conf.MailboxDescs

import scala.collection.mutable

/**
  * Created by andrey on 2/23/16.
  */
class AliasTable(mailboxDescs: MailboxDescs) {
  val aliasTable = new mutable.HashMap[String, String]

  for(desc <- mailboxDescs.descs) {
    for(alias <- desc.aliases) {
      aliasTable.put(alias, desc.name)
    }
  }

  def mapper(s: String): String = {
    aliasTable.getOrElse(s, s)
  }
}
