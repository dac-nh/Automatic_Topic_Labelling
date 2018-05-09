package Test

import java.io.File

/**
  * Created by Dark Son on 6/27/2017.
  */
object Test2 extends App() {
  /**
    * Tokenize
    */
  //  val paragraph: String = "A long paragraph here"
  //  val props: Properties = new Properties()
  //  props.put("annotators", "tokenize, ssplit, pos, lemma, stopword")
  //  props.setProperty("customAnnotatorClass.stopword", "intoxicant.analytics.coreNlp.StopwordAnnotator")
  //
  //  val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props, false)
  //  val annotation: Annotation = pipeline.process(paragraph)
  //  pipeline.prettyPrint(annotation, System.out)
  //
  //  def tokenizer(): Unit = {
  //    val ptbt = new PTBTokenizer(new FileReader("C:\\Projects\\13520173_NguyenHuuDac_FinalThesis\\conf\\stopword.txt"), new CoreLabelTokenFactory, "")
  //    while ( {
  //      ptbt.hasNext
  //    }) {
  //      val label = ptbt.next
  //      println(label)
  //    }
  //  }

  /**
    * List Folder and File
    */
  def tryListFoldersAndFile(): Unit = {
    val folderList = getListOfFolders("E:\\Developing\\Thesis\\topics_dir\\topics_dir")
    for (dir <- folderList) {
      getListOfFiles(dir.getPath).foreach(println(_))
    }
  }


  def getListOfFiles(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def getListOfFolders(dir: String): List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
      d.listFiles.filter(_.isDirectory).toList
    } else {
      List[File]()
    }
  }
}