---
out: scripting.html
---

### scripting

As much as it is fun watching our not-so-intelligent agent play the game on its own, it's a time-consuming and subjective process. The first thing we should do is to create a test harness that runs a preset game on a single thread with no UI. We will continuously let the agent suggest the best actions and execute them immediately.

We already have `PieceKind.apply(x: Int)`, so let's implement `toInt` on each `PieceKind`:

```scala
sealed trait PieceKind { def toInt: Int }
case object IKind extends PieceKind { def toInt = 0 }
case object JKind extends PieceKind { def toInt = 1 }
case object LKind extends PieceKind { def toInt = 2 }
case object OKind extends PieceKind { def toInt = 3 }
case object SKind extends PieceKind { def toInt = 4 }
case object TKind extends PieceKind { def toInt = 5 }
case object ZKind extends PieceKind { def toInt = 6 }
```

Now we can generate 1000 random numbers and print it on sbt shell:

```scala
package com.eed3si9n.tetrix

object Scripter {
  import Stage._

  def main(args: Array[String]) {
    args.toList match {
      case "generate" :: Nil => println(generateScript)
      case _ =>
        println("> run generate")
    }
  }
  def generateScript: String =
    (Stage.randomStream(new util.Random) map {
      _.toInt.toString} take {1000}) mkString
}
```

Let's run it:

```
library> run generate
[info] Running com.eed3si9n.tetrix.Scripter generate
41561156662214336116013264661323530534344000435311202344311644...
```

Copy-paste the output into `script/0.txt`. Run it four more times to create `1.txt`, .., `4.txt`. Next we want to script a game using the file.

```
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
    println(file.getName + ": " + s.lineCount.toString)
  }
```

I had to modify `Agent`'s `bestMove` method to `bestMoves`, and let it return `Seq[StageMessage]`.

We can now run from sbt shell:

```
library> run test
[info] Compiling 1 Scala source to /Users/eed3si9n/work/tetrix.scala/library/target/scala-2.10/classes...
[info] Running com.eed3si9n.tetrix.Scripter test
0.txt: 7 lines
1.txt: 5 lines
2.txt: 7 lines
3.txt: 9 lines
4.txt: 7 lines
[success] Total time: 61 s, completed Aug 17, 2012 10:17:14 PM
```

In 61 seconds we ran five games all resulting 7 +/- 2 lines. No matter how many times we run them the results will be the same. I am now interested in how the grid looked when the agent lost these games.

```scala
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
```

Here's one of the outputs:

```
xxxxxxx   
 xx xx x  
 xxx x x  
 xx xxxxx 
 xx  xxxx 
 xxxxxxxx 
 xxx x xx 
 xxx x xx 
 xxxxx xx 
 xxx x xx 
 xxxxx xx 
 xxxxxxxx 
 xxx xxxx 
 xxxxxxxx 
 xxxxxxxx 
 xxx x xx 
xxxxxxxxx 
 xxxxxxxx 
 xxxxxxxx 
xxxxxxxxx 
xxxxxxxxx 
 xxxxxxxxx
----------
```

The entire 10th column is empty except for the bottom row. Let's call these crevasses.
