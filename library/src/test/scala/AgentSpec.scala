import org.specs2._

class AgentSpec extends Specification with StateExample { def is =            s2"""
  This is a specification to check Agent

  Utility function should
    evaluate initial state as 0.0,                                            $utility1
    evaluate GameOver as -1000.0,                                             $utility2
    evaluate an active state by lastDeleted - 1                               $utility3

  Penalty function should
    penalize having blocks stacked up high                                    $penalty1
    penalize having blocks covering other blocks                              $penalty2
    penalize having blocks creating deep crevasses                            $penalty3

  ActionSeqs function should
    list out potential action sequences                                       $actionSeqs1

  Solver should
    pick MoveLeft for s1                                                      $solver1
    pick Drop for s3                                                          $solver2
    pick RotateCW for s5                                                      $solver3
                                                                              """
  
  import com.eed3si9n.tetrix._
  import Stage._

  val agent = new Agent

  def utility1 =
    agent.utility(s0) must_== 0.0 
  def utility2 =
    agent.utility(gameOverState) must_== -1000.0
  def utility3 = {
    val s = Function.chain(Nil padTo (19, tick))(s3)
    agent.reward(s) must_== 0.0
  }
  def penalty1 = {
    val s = newState(Seq(
      (0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must_== 77.0 
  } and {
    val s = newState(Seq((1, 0))
    map { Block(_, ZKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must_== 11.0
  }
  def penalty2 = {
    val s = newState(Seq(
      (0, 0), (2, 0), (0, 1), (1, 1), (2, 1), (3, 1))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must beCloseTo(44.09, 0.01) 
  }
  def penalty3 = {
    val s = newState(Seq(
      (0, 0), (1, 0), (1, 1), (1, 2), (1, 3), (1, 4))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must beCloseTo(75.13, 0.01) 
  }
  def actionSeqs1 = {
    val s = newState(Nil, (10, 20), TKind :: TKind :: Nil)
    val seqs = agent.actionSeqs(s)
    seqs.size must_== 32
  }
  val thinkTime = 1000
  def solver1 =
    agent.bestMove(s1, thinkTime) must_== MoveLeft
  def solver2 =
    agent.bestMove(s3, thinkTime) must beOneOf(Drop, Tick)
  def solver3 =
    agent.bestMove(s5, thinkTime) must_== RotateCW
}
