package com.chaschev.mail

import java.nio.file.{Files, Path, Paths}

import scala.collection.mutable

/**
  * Created by andrey on 2/3/16.
  */
case class CacheManager(){
  def initFolders: Unit = {
    Files.createDirectory(CacheManager.CACHE_PATH)
  }

  def initMailFolders(folders: List[MailServer]): Unit = {
    ???
  }
}

case object CacheManager {
  val CACHE_PATH = Paths.get(".cache")

  def makeFsSafe(s: String): String = s.replaceAll("[\\W^.]+", "")

  def getPath(server: String, mailbox: String, folder: String): Path = {
    import CacheManager.{makeFsSafe => safe}

    CacheManager.CACHE_PATH.resolve(Paths.get(safe(server), safe(mailbox), safe(folder)))
  }

  // initializes with an empty cache if doesn't exist
  def getFile(server: String, mailbox: String, folder: String): MailFolderCache = {
    val file = getPath(server, mailbox, folder).toFile

    if(file.exists()) return MailFolderCache.fromFile(file)

    val parent = file.getParentFile

    if(!parent.exists()){
      if(!parent.mkdirs()) throw new RuntimeException("unable to make a parent folder: " + parent.getAbsolutePath)
    }

    MailFolderCache(file, new mutable.MutableList[MailFolderEntryCached])
  }
}

