package com.goyeau.orchestra

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import autowire._
import com.goyeau.orchestra.pages.FolderBoardPage
import com.goyeau.orchestra.routes.WebRouter.{AppPage, BoardPage, TaskLogsPage}
import com.goyeau.orchestra.pages.{LogsPage, SingleTaskBoardPage}
import io.circe.{Decoder, Encoder}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router.{RouterConfigDsl, StaticDsl}
import japgolly.scalajs.react.vdom.html_<^._
import shapeless.HList
import io.circe.generic.auto._
import io.circe.syntax._

sealed trait Board {
  def pathName: String
  def name: String
  def route(implicit ec: ExecutionContext): StaticDsl.Rule[AppPage]
  def apiRoute(implicit ec: ExecutionContext): Route
}

case class FolderBoard(name: String, childBoards: Seq[Board]) extends Board {
  val pathName = name.toLowerCase

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[AppPage].buildRule { dsl =>
    import dsl._
    staticRoute(pathName, BoardPage(this)) ~>
      renderR(ctrl => FolderBoardPage.component(FolderBoardPage.Props(name, childBoards, ctrl))) |
      childBoards.map(_.route).reduce(_ | _).prefixPath_/(pathName)
  }

  def apiRoute(implicit ec: ExecutionContext) = childBoards.map(_.apiRoute).reduce(_ ~ _)
}

object FolderBoard {
  def apply(name: String): (Board*) => FolderBoard = (childBoards: Seq[Board]) => FolderBoard(name, childBoards)
}

case class SingleTaskBoard[Params <: HList, ParamValues: Encoder, Result: Decoder](
  name: String,
  task: Task[Params, ParamValues, Result]
)(implicit paramGetter: ParamGetter[Params, ParamValues])
    extends Board {
  val pathName = task.id.name

  def route(implicit ec: ExecutionContext) = RouterConfigDsl[AppPage].buildRule { dsl =>
    import dsl._
    (
      staticRoute(root, BoardPage(this)) ~> renderR { ctrl =>
        SingleTaskBoardPage.component(name, task, ctrl)
      } |
        dynamicRouteCT(uuid.caseClass[TaskLogsPage] / "logs") ~> dynRender(page => LogsPage.component(page, task))
    ).prefixPath_/(pathName)
  }

  def apiRoute(implicit ec: ExecutionContext) = path(task.id.name / Segments) { segments =>
    post(task.apiRoute(segments))
  }
}
