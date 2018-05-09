package services.tools

import java.io.File

import scala.tools.nsc.io.Path

/**
  * Created by Dark Son on 7/11/2017.
  */
object FileDirectionHandler {
  /**
    * Create files and directories
    */
  def createPath(url: String) {
    val path: Path = Path(url)

    // create file but fail if the file already exists.
    // an exception may be thrown
    //    path.createFile()

    // force create a file will fail if it is a directory which
    // contain children
    //    path.createFile(failIfExists=false)

    // TODO createFile with attributes

    // create a directory at the path location
    //    path.createDirectory()
    path.createDirectory(failIfExists = false)
  }

  /**
    * Delete files and directories
    */
  def deletePath(url: String) {
    val path: Path = Path("/tmp/file")
    // Delete path and all children.  This is currently not a safe method so
    // it should be used with caution.  Future versions will be better
    // by default it will throw an exception if one of the files cannot be deleted
    path.deleteRecursively()
  }

  /**
    * Check if file existed
    */
  def checkTextFile(path: String): Boolean = {
    val d = new File(path)
    if (d.exists && d.isDirectory) {
      val listFile = d.listFiles.filter(_.isFile).toList
      if (listFile.isEmpty) {
        false
      } else {
        true
      }
    } else {
      false
    }
  }

  /**
    * Get List File
    */
  def getListFile (path: String): List[File] = {
    val direction = new File(path)
    if (direction.exists && direction.isDirectory){
      direction.listFiles.filter(_.isFile).toList
    } else {
      List()
    }
  }
}
