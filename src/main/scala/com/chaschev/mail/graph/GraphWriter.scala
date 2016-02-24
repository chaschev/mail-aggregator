package com.chaschev.mail.graph

import java.io.PrintStream

import com.chaschev.mail.graph.GephiWriter._
import org.apache.logging.log4j.{LogManager, Logger}

import scala.collection.mutable

/**
  * Created by andrey on 2/23/16.
  */
trait GraphWriter {
  def write(graph: Graph, aliasTable: AliasTable, out1: PrintStream, out2: Option[PrintStream] = None)
}

object GephiWriter {
  val logger: Logger = LogManager.getLogger(getClass)
}

class GephiWriter extends GraphWriter {
  import GephiWriter.logger

  override def write(graph: Graph, aliasTable: AliasTable, out1: PrintStream, out2Opt: Option[PrintStream] = None): Unit = {
    out1.println("Id,Label,Interval")

    for(node <- graph.list) {
      out1.println(s"${node.num},${node.num} ${node.name},")
    }

    out1.close()

    val out2 = out2Opt.get

    out2.println("Source,Target,Type")

    val nodesCount = graph.list.length
    var edgesCount = 0

    for(node1 <- graph.list) {
      val nodes2  = graph.getNondirectSiblings(node1)

      if(graph.isIsolated(node1)) {
        logger.warn(s"$node1 is isolated")
      }

      for(node2 <- nodes2) {
        out2.println(s"${node1.num},${node2.num},Undirected")
        edgesCount += 1
      }
    }

    out2.close()

    println(s"done. nodes: $nodesCount, edges: $edgesCount")
  }
}

object GraphvizWriter {
  val logger: Logger = LogManager.getLogger(getClass)
}

class GraphvizWriter extends GraphWriter {
  import GraphvizWriter.logger

  override def write(graph: Graph, aliasTable: AliasTable, out: PrintStream, out2: Option[PrintStream] = None): Unit = {
    out.println(
      """graph {
 forcelabels=true;
""")

    var edgesCount = 0

    for(node1 <- graph.list) {
      out.println(s"""${node1.num} [label="${node1.name}"];""")
    }

    for(node1 <- graph.list) {
      val nodes2  = graph.getNondirectSiblings(node1)

      if(nodes2.nonEmpty) {
        out.println(s" ${node1.num} -- {${nodes2.map{_.num}.mkString(" ")}};")
        edgesCount += nodes2.size
      }

      if(graph.isIsolated(node1)) {
        logger.warn(s"$node1 is isolated")
      }
    }

    out.println("}")

    out.close()

    println(s"done. nodes: ${graph.list.length}, edges: $edgesCount")
  }
}



