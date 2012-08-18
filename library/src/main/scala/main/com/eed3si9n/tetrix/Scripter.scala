package com.eed3si9n.tetrix

object Scripter {
  import Stage._

  def main(args: Array[String]) {
    args.toList match {
      case "generate" :: Nil => println(generateScript)
      case "test" :: Nil     => testAll
      case _ =>
        println("> run test")
        println("> run generate")
    }
  }
  def testAll {
    val scriptDir = new java.io.File("script")
    for (i <- 0 to 4)
      test(new java.io.File(scriptDir, i.toString + ".txt"))
  }
  def test(file: java.io.File) {
    val pieces = io.Source.fromFile(file).seq map { c =>
      PieceKind(c.toString.toInt)
    }
    val s0 = newState(Nil, (10, 23), pieces.toSeq)
    var s: GameState = s0
    val agent = new Agent
    while (s.status != GameOver) {
      val ms = agent.bestMoves(s)
      s = Function.chain(ms map {toTrans})(s)
    }
    println(file.getName + ": " + s.lineCount.toString + " lines")
  }

  def generateScript: String =
    (Stage.randomStream(new util.Random) map {
      _.toInt.toString} take {1000}) mkString
}
