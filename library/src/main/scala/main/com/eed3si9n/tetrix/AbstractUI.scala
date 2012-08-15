package com.eed3si9n.tetrix

class AbstractUI {
  import akka.actor._
  import akka.pattern.ask
  import akka.util.duration._
  import akka.util.Timeout
  import akka.dispatch.{Future, Await}
  import scala.collection.immutable.Stream
  implicit val timeout = Timeout(100 millisecond)

  private[this] val initialState = Stage.newState(Block((0, 0), TKind) :: Nil,
    (10, 23), randomStream(new util.Random))
  private[this] val system = ActorSystem("TetrixSystem")
  private[this] val stateActor = system.actorOf(Props(new StateActor(
    initialState)), name = "stateActor")
  private[this] val playerActor = system.actorOf(Props(new StageActor(
    stateActor)), name = "playerActor")
  private[this] val agentActor = system.actorOf(Props(new AgentActor(
    playerActor)), name = "agentActor")
  private[this] val masterActor = system.actorOf(Props(new GameMasterActor(
    stateActor, agentActor)), name = "masterActor")
  private[this] val tickTimer = system.scheduler.schedule(
    0 millisecond, 700 millisecond, playerActor, Tick)
  private[this] val masterTickTimer = system.scheduler.schedule(
    0 millisecond, 700 millisecond, masterActor, Tick)
  private[this] def randomStream(random: util.Random): Stream[PieceKind] =
    PieceKind(random.nextInt % 7) #:: randomStream(random)

  def left()  { playerActor ! MoveLeft }
  def right() { playerActor ! MoveRight }
  def up()    { playerActor ! RotateCW }
  def down()  { playerActor ! Tick }
  def space() { playerActor ! Drop }
  def view: GameView =
    Await.result((stateActor ? GetView).mapTo[GameView], timeout.duration)
}
