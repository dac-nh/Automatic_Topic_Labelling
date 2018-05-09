package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc.{Action, Controller}
import services.TopicService

@Singleton
class TopicController @Inject()(topicService: TopicService) extends Controller {
  def main = Action {
    Ok(
      views.html.topicNeo4jSource("List ACM Topics - 1998 - Neo4j")
    )
  }

  def topicLabelingGetPage = Action {
    Ok(
      views.html.topicLabelingNeo4jSource("Topic Labeling - ACM 1998")
    )
  }

  def getListTopicNeo4j = Action {
    Ok(
      topicService.getTopicListNeo4j
    )
  }

  def getListKeywordOfTopicNeo4j = Action { request =>
      val topicLabel = request.body.asFormUrlEncoded.get("topicLabel").head
      Ok(
        topicService.getListKeyword(topicLabel)
      )
  }

  def topicLabelingPost = Action(parse.multipartFormData) { request =>
    val filePath = request.body.file("file").get.ref.file.getPath
    val level = request.body.asFormUrlEncoded("level").head.toInt
    Ok(
      topicService.topicLabelingForEachDocument(filePath, level)
    )
  }
}
