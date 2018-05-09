package services.tools

import play.api.libs.json.{JsArray, JsValue, Json}

/**
  * Created by Dark Son on 7/9/2017.
  */
class JsonService {
  def toJsValue(): JsValue = {
    val result: JsValue = Json.obj()
    result
  }

  def toJsArray(): JsArray = {
    var result: JsArray = Json.arr()
    result
  }
}
