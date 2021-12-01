package com.parER.akka.streams

import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}
import com.parER.datastructure.Comparison

import scala.concurrent.{Future, Promise}

class CollectorSink(threshold: Double = 0.5) extends GraphStageWithMaterializedValue[SinkShape[List[Comparison]], Future[(Long, List[Comparison])]] {
  val in: Inlet[List[Comparison]] = Inlet("CollectorSink")
  override val shape: SinkShape[List[Comparison]] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes) = {
    val p: Promise[(Long, List[Comparison])] = Promise()
    val logic = new GraphStageLogic(shape) {

      val buffer = List.newBuilder[Comparison]
      var counter = 0L;

      // This requests one element at the Sink startup.
      override def preStart(): Unit = pull(in)

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val comparisons = grab(in)
          counter += comparisons.size
          buffer ++= comparisons.filter(c => c.sim >= threshold)
          pull(in)
        }

        override def onUpstreamFinish(): Unit = {
          val result = (counter, buffer.result())
          p.trySuccess(result)
          completeStage()
        }
      })
    }
    (logic, p.future)
  }
}
