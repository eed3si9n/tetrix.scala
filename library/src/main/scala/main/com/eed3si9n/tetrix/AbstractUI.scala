package com.eed3si9n.tetrix

class AbstractUI {
  import akka.actor._
  import akka.actor.Actor._
  import akka.util.duration._
  import akka.dispatch.Future
  import scala.collection.immutable.Stream
  implicit val timeout = Timeout(100 millisecond)

  private[this] val initialState = Stage.newState(Nil,
    (10, 23), Stage.randomStream(new util.Random))
  private[this] val stateActor1 = actorOf(new StateActor(
    initialState)).start()
  private[this] val stageActor1 = actorOf(new StageActor(
    stateActor1) {
    self.id = "stageActor1"
  }).start()
  private[this] val stateActor2 = actorOf(new StateActor(
    initialState) {
    self.id = "stageActor2"
  }).start()
  private[this] val stageActor2 = actorOf(new StageActor(
    stateActor2)).start()
  private[this] val agentActor = actorOf(new AgentActor(
    stageActor2)).start()
  private[this] val masterActor = actorOf(new GameMasterActor(
    stageActor1, stageActor2, stateActor1, stateActor2, agentActor)).start()
  private[this] val gravityActor = actorOf(new GameMasterActor(
    stageActor1, stageActor2, stateActor1, stateActor2, agentActor)).start()
  
  masterActor ! Start
  gravityActor ! GravityTimer

  def left()  { stageActor1 ! MoveLeft }
  def right() { stageActor1 ! MoveRight }
  def up()    { stageActor1 ! RotateCW }
  def down()  { stageActor1 ! Tick }
  def space() { stageActor1 ! Drop }
  def views: (GameView, GameView) =
    ((stateActor1 ? GetView).mapTo[GameView].get,
    (stateActor2 ? GetView).mapTo[GameView].get)
}
