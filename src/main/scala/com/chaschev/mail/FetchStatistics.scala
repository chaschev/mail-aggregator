package com.chaschev.mail

import java.util.concurrent.atomic.AtomicInteger

/**
  * Created by andrey on 2/24/16.
  */
case class FetchStatistics(
  var errors: AtomicInteger = new AtomicInteger(),
  var skippedAttachments: AtomicInteger = new AtomicInteger(),
  var total: AtomicInteger = new AtomicInteger()
) {
  override def toString: String = s"""FetchStatistics(errors=$errors, skippedAttachments=$skippedAttachments, total=$total)"""
}
