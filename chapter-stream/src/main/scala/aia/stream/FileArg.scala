package aia.stream

import java.nio.file.{ Path, Paths }

object FileArg {
  def shellExpanded(path: String): Path = {
    if(path.startsWith("~/")) {
      Paths.get(System.getProperty("user.home"), path.drop(2))
    } else {
      Paths.get(path)      
    }
  }
}