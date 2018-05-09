package services.tools.algorithm

import breeze.numerics.{abs, log, log10}

/**
  * Created by Dark Son on 7/17/2017.
  */
object KullbackLeiblerDivergence {
  var positiveInfinity: Int = 0

  def run (x: Map[String, Double], y: Map[String, Double]): Double ={
    abs(execute(x,y))
  }
  def execute(x: Map[String, Double], y: Map[String, Double]): Double = {
    if (x.isEmpty) 0.0
    else {
      execute(x.head._2, y.head._2) + execute(x.tail, y.tail)
    }
  }

  def execute(x: Double, y: Double): Double = {
    var result: Double = 0
    if (x == 0 || y == 0) {
      return 0
    } else if (y == 0) {
      positiveInfinity += 1
      result = x * (log(x) + 2)
    } else {
      result = x * log(x / y)
    }
    result
  }
}
