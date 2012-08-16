import org.specs2._

class AgentSpec extends Specification with StateExample { def is = sequential ^
  "This is a specification to check Agent"                  ^
                                                            p^
  "Utility function should"                                 ^
    """evaluate initial state as 0.0,"""                    ! utility1^
    """evaluate GameOver as -1000.0,"""                     ! utility2^
    """evaluate an active state by lineCount"""             ! utility3^
                                                            p^
  "Penalty function should"                                 ^  
    """penalize having blocks stacked up high"""            ! penalty1^
                                                            p^
  "ActionSeqs function should"                              ^  
    """list out potential action sequences"""               ! actionSeqs1^
                                                            p^
  "Solver should"                                           ^
    """pick MoveLeft for s1"""                              ! solver1^
    """pick Drop for s3"""                                  ! solver2^
    """pick RotateCW for s5"""                              ! solver3^
                                                            end
  
  import com.eed3si9n.tetrix._
  import Stage._

  val agent = new Agent

  def utility1 =
    agent.utility(s0) must_== 0.0 
  def utility2 =
    agent.utility(gameOverState) must_== -1000.0
  def utility3 = {
    val s = Function.chain(Nil padTo (19, tick))(s3)
    agent.reward(s) must_== 1.0
  }
  def penalty1 = {
    val s = newState(Seq(
      (0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must_== 7.0 
  } and {
    val s = newState(Seq((1, 0))
    map { Block(_, ZKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.penalty(s) must_== 1.0
  }
  def actionSeqs1 = {
    val s = newState(Nil, (10, 20), TKind :: TKind :: Nil)
    val seqs = agent.actionSeqs(s)
    seqs.size must_== 32
  }
  def solver1 =
    agent.bestMove(s1) must_== MoveLeft
  def solver2 =
    agent.bestMove(s3) must beOneOf(Drop, Tick)
  def solver3 =
    agent.bestMove(s5) must_== RotateCW
}
