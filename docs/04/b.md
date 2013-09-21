---
out: akka.html
---

  [akka]: http://doc.akka.io/docs/akka/2.0.2/intro/getting-started-first-scala.html

### akka

Another way of managing concurrency is to use message passing framework like Akka actor. See [Getting Started Tutorial (Scala): First Chapter][akka] for an intro to actors. We can follow the steps in the tutorial.

First, add `"akka-actor"` to sbt:

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

Next, create actors.scala and define message types.

```scala
sealed trait StageMessage
case object MoveLeft extends StageMessage
case object MoveRight extends StageMessage
case object RotateCW extends StageMessage
case object Tick extends StageMessage
case object Drop extends StageMessage
case object View extends StageMessage
```

Then create `StageActor` to handle the messages. 

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

We can now rewire the abstract UI to use an Akka actor internally:

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

The mutation is now wrapped inside `playerActor`, which is guaranteed to handle messages one at a time. Also, note that the timer is replaced with a schedule. Overall, the message passing allows us to reason about concurrent behavior in a resonable way.
