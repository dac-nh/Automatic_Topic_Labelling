package services.tools.algorithm

import breeze.numerics.sqrt

object Euclidean {
  def run(x: Map[String, Double], y: Map[String, Double]): Double = {
    sqrt(execute(x, y))
  }

  def execute(x: Map[String, Double], y: Map[String, Double]): Double = {
    if (x.isEmpty) {
      return 0.0
    }

    def executeDouble(x: Double, y: Double): Double = {
        (x - y) * (x - y)
    }

    val distance: Double = executeDouble(x.head._2, y.head._2) + execute(x.tail, y.tail)
    distance
  }
}
