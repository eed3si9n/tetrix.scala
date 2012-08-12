package com.eed3si9n.tetrix

class AbstractUI {
  import akka.actor._
  import akka.pattern.ask
  import akka.util.duration._
  import akka.util.Timeout
  import akka.dispatch.{Future, Await}
  import scala.collection.immutable.Stream
  implicit val timeout = Timeout(1 second)

  private[this] val initialState = Stage.newState(Block((0, 0), TKind) :: Nil,
    randomStream(new util.Random))
  private[this] val system = ActorSystem("TetrixSystem")
  private[this] val playerActor = system.actorOf(Props(new StageActor(
    initialState)), name = "playerActor")
  private[this] val timer = system.scheduler.schedule(
    0 millisecond, 1000 millisecond, playerActor, Tick)
  private[this] def randomStream(random: util.Random): Stream[PieceKind] =
    PieceKind(random.nextInt % 7) #:: randomStream(random)

  def left()  { playerActor ! MoveLeft }
  def right() { playerActor ! MoveRight }
  def up()    { playerActor ! RotateCW }
  def down()  { playerActor ! Tick }
  def space() { playerActor ! Drop }
  def view: GameView =
    Await.result((playerActor ? View).mapTo[GameView], timeout.duration)
}
