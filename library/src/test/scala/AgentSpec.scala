import org.specs2._

class AgentSpec extends Specification with StateExample { def is = sequential ^
  "This is a specification to check Agent"                  ^
                                                            p^
  "Utility function should"                                 ^
    """evaluate initial state as 0.0,"""                    ! utility1^
    """evaluate GameOver as -1000.0,"""                     ! utility2^
    """evaluate an active state by lineCount"""             ! utility3^
    """penalize having gaps between the columns"""          ! utility4^
                                                            p^
  "Solver should"                                           ^
    """pick MoveLeft for s1"""                              ! solver1^
    """pick Drop for s3"""                                  ! solver2^
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
    agent.utility(s) must_== 1.0
  }
  def utility4 = {
    val s = newState(Seq(
      (0, 0), (0, 1), (0, 2), (0, 3), (0, 4), (0, 5), (0, 6))
      map { Block(_, TKind) }, (10, 20), TKind :: TKind :: Nil)
    agent.utility(s) must_== -36.0
  }
  def solver1 =
    agent.bestMove(s1) must_== MoveLeft
  def solver2 =
    agent.bestMove(s3) must beOneOf(Drop, Tick)
}
