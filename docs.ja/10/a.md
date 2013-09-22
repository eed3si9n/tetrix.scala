---
out: scripting.html
---

### スクリプティング

自分で書いたエージェントがゲームをプレイするのを眺めているは確かに楽しいけれども、時間がかかり、主観的なプロセスだ。今日まず取り組むべきなのはプリセットされたゲームを UI無しのシングルスレッド上で実行するテストハーネスだ。エージェントに絶え間なく最良のアクションを計算させて即時に実行していく。

既に `PieceKind.apply(x: Int)` があるから、`PieceKind` にそれぞれ `toInt` を実装しよう:

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

これで 1000個のランダムな整数を生成して sbt シェルに表示できる:

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

実行してみよう:

```
library> run generate
[info] Running com.eed3si9n.tetrix.Scripter generate
41561156662214336116013264661323530534344000435311202344311644...
```

表示されたアウトプットを `script/0.txt` というファイルにコピペする。あと四回実行してそれぞれ `1.txt`、..、`4.txt` に保存する。次にファイルを使ってゲームをスクリプト化する。

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

`Agent` の `bestMove` に手を加えて `Seq[StageMessage]` を返す `bestMoves` にした。

これで sbt シェルから実行できる:

```
library> run test
[info] Compiling 1 Scala source to /Users/eed3si9n/work/tetrix.scala/library/target/scala-2.9.2/classes...
[info] Running com.eed3si9n.tetrix.Scripter test
0.txt: 7 lines
1.txt: 5 lines
2.txt: 7 lines
3.txt: 9 lines
4.txt: 7 lines
[success] Total time: 61 s, completed Aug 17, 2012 10:17:14 PM
```

61秒間で 5つのゲームを実行して 7 +/- 2 ラインという結果となった。何回実行してもこれは同じ結果を返す。エージェントがゲームに負けた時のグリッドの様子を見てみよう。

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

アウトプットの例だ:

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

最下行以外の全ての 10番目の列が空になっている。これらをクレバスと呼ぼう。
