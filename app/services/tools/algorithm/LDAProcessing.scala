package services.tools.algorithm

import java.io.File

import Library.GeneralConstant
import edu.stanford.nlp.process.Morphology
import edu.stanford.nlp.simple.Document
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.{Vector => MLVector}
import org.apache.spark.mllib.clustering.{DistributedLDAModel, EMLDAOptimizer, LDA, OnlineLDAOptimizer}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConversions._

case class Params(
                   input: String = "",
                   k: Int = 1,
                   maxIterations: Int = 1,
                   docConcentration: Double = -1,
                   topicConcentration: Double = -1,
                   vocabSize: Int = 2900000,
                   stopwordFile: String = GeneralConstant.STOPWORD_PATH,
                   algorithm: String = "em",
                   checkpointDir: Option[String] = None,
                   checkpointInterval: Int = 10,
                   maxTermPerTopic: Int = 35)

class LDAProcessing(sc: SparkContext, spark: SparkSession) {
  def run(params: Params): Map[Double, List[String]] = {
    var result: Map[Double, List[String]] = Map()
    //Logger.getRootLogger.setLevel(Level.WARN)

    // Load documents, and prepare them for LDA.
    val preprocessStart = System.nanoTime()
    val (corpus, vocabArray, actualNumTokens) =
      preprocess(sc, params.input, params.vocabSize, params.stopwordFile)
    val actualCorpusSize = corpus.count()
    val actualVocabSize = vocabArray.length
    val preprocessElapsed = (System.nanoTime() - preprocessStart) / 1e9
    corpus.cache()
    println()
    println(s"Corpus summary:")
    println(s"\t Training set size: $actualCorpusSize documents")
    println(s"\t Vocabulary size: $actualVocabSize terms")
    println(s"\t Training set size: $actualNumTokens tokens")
    println(s"\t Preprocessing time: $preprocessElapsed sec")
    println()

    // Run LDA.
    val lda = new LDA()

    val optimizer = params.algorithm.toLowerCase match {
      case "em" => new EMLDAOptimizer
      // add (1.0 / actualCorpusSize) to MiniBatchFraction be more robust on tiny datasets.
      case "online" => new OnlineLDAOptimizer().setMiniBatchFraction(0.05 + 1.0 / actualCorpusSize)
      case _ => throw new IllegalArgumentException(
        s"Only em, online are supported but got ${params.algorithm}.")
    }

    lda.setOptimizer(optimizer)
      .setK(params.k)
      .setMaxIterations(params.maxIterations)
      .setDocConcentration(params.docConcentration)
      .setTopicConcentration(params.topicConcentration)
      .setCheckpointInterval(params.checkpointInterval)
    if (params.checkpointDir.nonEmpty) {
      sc.setCheckpointDir(params.checkpointDir.get)
    }
    val startTime = System.nanoTime()
    val ldaModel = lda.run(corpus)
    val elapsed = (System.nanoTime() - startTime) / 1e9

    println(s"Finished training LDA model.  Summary:")
    println(s"\t Training time: $elapsed sec")
    ldaModel match {
      case distLDAModel: DistributedLDAModel =>
        val avgLogLikelihood = distLDAModel.logLikelihood / actualCorpusSize.toDouble
        println(s"\t Training data average log likelihood: $avgLogLikelihood")
        println()
      case _ =>
    }

    // Print the topics, showing the top-weighted terms for each topic.
    val topicIndices = ldaModel.describeTopics(maxTermsPerTopic = params.maxTermPerTopic)
    val topics = topicIndices.map { case (terms, termWeights) =>
      terms.zip(termWeights).map { case (term, weight) => (vocabArray(term.toInt), weight) }
    }
    // Dac: theta
    val thetaArray = new Array[Double](params.k) // Topic distribution
    ldaModel.asInstanceOf[DistributedLDAModel]
      .topicDistributions
      .collect()
      .foreach { case (documentId, topicDistributionVector) =>
        topicDistributionVector.foreachActive { case (id, value) =>
          thetaArray.update(id, value + thetaArray(id))
        }
      }

    /** 27-6-2017: Dac: get list string result of key and weigth **/

    println(s"${params.k} topics:")
    topics.zipWithIndex.foreach { case (topic, i) =>
      var listWord = List[String]() // List keyword of each topic
      println(s"TOPIC $i")
      topic.foreach { case (term, weight) =>
        println(s"$term\t$weight")
        listWord ::= s"$term\t$weight" // add keyword to list keyword
        result += (thetaArray(i) -> listWord)
      }
    }
    // Will be stopped at the end of main object
    //        sc.stop()
    result
  }


  /**
    * Load documents, tokenize them, create vocabulary, and prepare documents as term count vectors.
    *
    * @return (corpus, vocabulary as array, total token count in corpus)
    */
  def preprocess(
                  sc: SparkContext,
                  paths: String,
                  vocabSize: Int,
                  stopwordFile: String): (RDD[(Long, Vector)], Array[String], Long) = {

    import spark.implicits._
    //Reading the Whole Text Files
    val initialrdd = spark.sparkContext.wholeTextFiles(paths).map(_._2)
    initialrdd.cache()
    val rdd = initialrdd.mapPartitions { partition =>
      val morphology = new Morphology()
      partition.map { value =>
        LDAHelper.getLemmaText(value, morphology)
      }
    }.map(LDAHelper.filterSpecialCharacters)
    rdd.cache()
    initialrdd.unpersist()
    val df = rdd.toDF("docs")
    var customizedStopWords: Array[String] = if (stopwordFile.isEmpty) {
      Array.empty[String]
    } else {
      val stopWordText = sc.textFile(stopwordFile).collect()
      stopWordText.flatMap(_.stripMargin.split(","))
    }
    //Tokenizing using the RegexTokenizer
    val tokenizer = new RegexTokenizer().setInputCol("docs").setOutputCol("rawTokens")

    //Removing the Stop-words using the Stop Words remover
    val stopWordsRemover = new StopWordsRemover().setInputCol("rawTokens").setOutputCol("tokens")
    customizedStopWords = customizedStopWords :+ "rrb" :+ "lrb" :+ "algorithm" :+ "rsb" :+ "lsb" :+ "edit" // Trick to remove non-sense keywords
    stopWordsRemover.setStopWords(stopWordsRemover.getStopWords ++ customizedStopWords)

    //Converting the Tokens into the CountVector
    val countVectorizer = new CountVectorizer().setVocabSize(vocabSize).setInputCol("tokens").setOutputCol("features")

    //Setting up the pipeline
    val pipeline = new Pipeline().setStages(Array(tokenizer, stopWordsRemover, countVectorizer))

    val model = pipeline.fit(df)
    val documents = model.transform(df).select("features").rdd.map {
      case Row(features: MLVector) => Vectors.fromML(features)
    }.zipWithIndex().map(_.swap)

    (documents,
      model.stages(2).asInstanceOf[CountVectorizerModel].vocabulary, // vocabulary
      documents.map(_._2.numActives).sum().toLong) // total token count
  }
}

object LDAProcessing extends App {
  // Start LDA with variable path
  def useLDA(url: String): List[String] = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop") // Set Hadoop path
    val conf = new SparkConf().setAppName(s"LDAProcess").setMaster("local[*]").set("spark.executor.memory", "2g")
    val spark = SparkSession.builder().config(conf).getOrCreate()
    val sc = spark.sparkContext
    val lda = new LDAProcessing(sc, spark)
    val result = run(lda, new File(url).getPath)

    spark.stop()
    result
  }

  // Use LDA for Topic Labeling
  def useLDAForTopicLabeling(url: String): List[String] = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop") // Set Hadoop path
    val conf = new SparkConf().setAppName(s"LDAProcess").setMaster("local[*]").set("spark.executor.memory", "2g")
    val spark = SparkSession.builder().config(conf).getOrCreate()
    val sc = spark.sparkContext
    val lda = new LDAProcessing(sc, spark)
    val result = runForTopicLabeling(lda, new File(url).getPath)

    spark.stop()
    result
  }

  def run(lda: LDAProcessing, path: String): List[String] = {
    val defaultParams = Params().copy(input = path, maxTermPerTopic = 50)
    lda.run(defaultParams).head._2
  }

  // For Topic Labeling
  def runForTopicLabeling(lda: LDAProcessing, path: String): List[String] = {
    val defaultParams = Params().copy(input = path, k = 1, maxTermPerTopic = 50)
    lda.run(defaultParams).head._2
  }
}

object LDAHelper {
  def filterSpecialCharacters(document: String) = document.replaceAll("""[0-9 ! @ # $ % ^ & * ( ) _ + - − , " ' ; : . ` ? --]""", " ")

  def getStemmedText(document: String) = {
    val morphology = new Morphology()
    new Document(document).sentences().toList.flatMap(_.words().toList.map(morphology.stem)).mkString(" ")
  }

  def getLemmaText(document: String, morphology: Morphology) = {
    val string = new StringBuilder()
    val value = new Document(document).sentences().toList.flatMap { a =>
      val words = a.words().toList
      val tags = a.posTags().toList
      (words zip tags).toMap.map { a =>
        val newWord = morphology.lemma(a._1, a._2)
        val addedWoed = if (newWord.length > 3) {
          newWord
        } else {
          ""
        }
        string.append(addedWoed + " ")
      }
    }
    string.toString()
  }
}