package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.Files
import play.api.mvc.{Action, Controller, MultipartFormData}
import services.{CategoryService, TopicService}

/**
  * Created by Dark Son on 6/25/2017.
  */
@Singleton
class CategoryController @Inject()(categoryService: CategoryService, topicService: TopicService) extends Controller {
  def main = Action {
    Ok(
      views.html.topicOrientDbSource("List ACM Topics - 2012 - OrientDb")
    )
  }

  def getListTopic = Action {
    Ok(
      categoryService.getListTopicsAsJsonArray
    )
  }

  def getListKeywordOfTopic = Action { request =>
    val topicId = request.body.asFormUrlEncoded.get("topicId").head
    Ok(
      categoryService.getListKeywordOfTopic(topicId.toString)
    )
  }

  def getAllTopicLabel = Action {
    Ok(
      categoryService.getAllTopicLabel
    )
  }

  def getListDocumentOfTopic = Action { request =>
    val topicId = request.body.asFormUrlEncoded.get("topicId").head
    Ok(
      categoryService.getListDocumentOfTopic(topicId.toString)
    )
  }

  // Get paper's abstract
  def getPaperAbstract = Action { request =>
    val paperId = request.body.asFormUrlEncoded.get("id").head
    Ok(
      categoryService.getPaperAbstract(paperId)
    )
  }

  // Search topic
  def getTopicByLabel = Action { request =>
    val label = request.body.asFormUrlEncoded.get("topicLabel").head
    Ok(
      categoryService.getTopicAndSubTopicByLabel(label)
    )
  }

  def index = Action {
    Ok(
      views.html.myInformationSource("Student Information")
    )
  }

  def topicLabelingView = Action {
    Ok(
      views.html.topicLabellingOrientDbSource("Topic Labelling - The 2012 ACM")
    )
  }

  def topicLabeling: Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { request =>
    val filePath = request.body.file("file").get.ref.file.getPath
    Ok(
//      categoryService.runForAccurateTesing() // for folder of documents
      categoryService.topicLabeling(filePath) // topic labelling for a document
    )
  }

  def estimateTopicsForVector = Action { request =>
    val listKeyword = request.body.asFormUrlEncoded.get("listKeyword[]").toList
    Ok(
      categoryService.estimateTopicsForVector(listKeyword)
    )
  }
}
