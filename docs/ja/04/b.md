---
out: akka.html
---

  [akka]: http://doc.akka.io/docs/akka/2.0.2/intro/getting-started-first-scala.html
  [amazon]: http://www.amazon.co.jp/dp/4798125415
  [amazon2]: http://www.amazon.co.jp/exec/obidos/ASIN/4797337206/tyano-22/

### akka

並行性を管理するもう一つの方法は Akka アクターのようなメッセージパッシングフレームワークを用いることだ。アクターの入門としては英語だと [Getting Started Tutorial (Scala): First Chapter][akka]のチュートリアルをたどっていくだけでアクターが書けるようになる。日本語だと [Scala 逆引きレシピ][amazon]の第9章「175: Akkaで並行処理を行いたい」などが参考になる。

まず `"akka-actor"` を sbt に追加する:

```scala
  resolvers ++= Seq(
    Resolver.sonatypeRepo("public"),
    Resolver.typesafeRepo("releases")
  )

...

lazy val specs2version = "2.2.2"
lazy val akkaVersion = "2.2.1"
lazy val libDeps = Def.setting { Seq(
  "org.specs2" %% "specs2" % specs2version % "test",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion
)}

lazy val library = (project in file("library")).
  settings(buildSettings: _*).
  settings(
    libraryDependencies ++= libDeps.value
  )
```

次に actors.scala を始めて、メッセージ型を定義する。

```scala
sealed trait StageMessage
case object MoveLeft extends StageMessage
case object MoveRight extends StageMessage
case object RotateCW extends StageMessage
case object Tick extends StageMessage
case object Drop extends StageMessage
case object View extends StageMessage
```

メッセージを処理するための `StageActor` を定義する。

```scala
class StageActor(s0: GameState) extends Actor {
  import Stage._

  private[this] var state: GameState = s0

  def receive = {
    case MoveLeft  => state = moveLeft(state)
    case MoveRight => state = moveRight(state)
    case RotateCW  => state = rotateCW(state)
    case Tick      => state = tick(state)
    case Drop      => state = drop(state)
    case View      => sender ! state.view
  }
}
```

これで抽象UI を再配線して内部で Akka アクターを使うように書き換えることができる:

```scala
package com.eed3si9n.tetrix

class AbstractUI {
  import akka.actor._
  import akka.pattern.ask
  import scala.concurrent.duration._
  import akka.util.Timeout
  import scala.concurrent._
  implicit val timeout = Timeout(1 second)
  import ExecutionContext.Implicits.global

  private[this] val initialState = Stage.newState(Block((0, 0), TKind) :: Nil,
    randomStream(new scala.util.Random))
  private[this] val system = ActorSystem("TetrixSystem")
  private[this] val playerActor = system.actorOf(Props(new StageActor(
    initialState)), name = "playerActor")
  private[this] val timer = system.scheduler.schedule(
    0 millisecond, 1000 millisecond, playerActor, Tick)
  private[this] def randomStream(random: scala.util.Random): Stream[PieceKind] =
    PieceKind(random.nextInt % 7) #:: randomStream(random)

  def left()  { playerActor ! MoveLeft }
  def right() { playerActor ! MoveRight }
  def up()    { playerActor ! RotateCW }
  def down()  { playerActor ! Tick }
  def space() { playerActor ! Drop }
  def view: GameView =
    Await.result((playerActor ? View).mapTo[GameView], timeout.duration)
}
```

これで可変性は `playerActor` で保護され、これは一度に一つづつのメッセージを取り扱うことが保証されている。また、タイマーがスケジュールに置き換えられたことにも注意してほしい。全般的に、メッセージパッシングを使うことで並行処理における振る舞いをより手軽に推論できるようになったと思う。
