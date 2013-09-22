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
    val results = for (i <- 0 to 4)
      yield test(new java.io.File(scriptDir, i.toString + ".txt"))
    println("lines  : " + results.map(_._1).toString)
    println("attacks: " + results.map(_._2).toString) 
  }
  def test(file: java.io.File): (Int, Int) = {
    val pieces = io.Source.fromFile(file).seq map { c =>
      PieceKind(c.toString.toInt)
    }
    val s0 = newState(Nil, (10, 23), pieces.toSeq)
    var s: GameState = s0
    val agent = new Agent
    while (s.status == ActiveStatus) {
      val ms = agent.bestMoves(s, 0)
      s = Function.chain(ms map {toTrans})(s)
    }
    printState(s)
    println(file.getName + ": " +
      s.lineCount.toString + " lines; " +
      s.attackCount.toString + " attacks")
    (s.lineCount, s.attackCount)
  }
  def printState(s: GameState) {
    val poss = s.blocks map {_.pos}
    (0 to s.gridSize._2 - 1).reverse foreach { y =>
      (0 to s.gridSize._1 - 1) foreach { x =>
        if (poss contains (x, y)) print("x")
        else print(" ")
      }
      println("")
      if (y == 0) (0 to s.gridSize._1 - 1) foreach { x =>
        print("-") }
    }
    println("")
  }
  def generateScript: String =
    (Stage.randomStream(new util.Random) map {
      _.toInt.toString} take {1000}) mkString
}
