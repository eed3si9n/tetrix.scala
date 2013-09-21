import org.specs2._

class StageSpec extends Specification { def is =                              s2"""
  This is a specification to check Stage

  Moving to the left the current piece should
    change the blocks in the view                                             $left1
    as long as it doesn't hit the wall.                                       $leftWall1

  Moving to the right the current piece should
    change the blocks in the view.                                            $right1

  Rotating the current piece should
    change the blocks in the view.                                            $rotate1

  Ticking the current piece should
    change the blocks in the view,                                            $tick1
    or spawn a new piece when it hits something.                              $tick2
    It should also clear out full rows.                                       $tick3
                                                                              """
  
  import com.eed3si9n.tetrix._
  import Stage._
  val s1 = newState(Block((0, 0), TKind) :: Nil)
  val s2 = newState(Block((3, 17), TKind) :: Nil)
  val s3 = newState(Seq(
      (0, 0), (1, 0), (2, 0), (3, 0), (7, 0), (8, 0), (9, 0))
    map { Block(_, TKind) })
  def left1 =
    moveLeft(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (3, 17), (4, 17), (5, 17), (4, 18)
    )).inOrder
  def leftWall1 =
    Function.chain(moveLeft :: moveLeft :: moveLeft ::
        moveLeft :: moveLeft :: Nil)(s1).
      blocks map {_.pos} must contain(exactly(
      (0, 0), (0, 17), (1, 17), (2, 17), (1, 18)
    )).inOrder
  def leftHit1 =
    moveLeft(s2).blocks map {_.pos} must contain(exactly(
      (3, 17), (4, 17), (5, 17), (6, 17), (5, 18)
    )).inOrder
  def right1 =
    moveRight(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (5, 17), (6, 17), (7, 17), (6, 18)
    )).inOrder
  def rotate1 =
    rotateCW(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (5, 18), (5, 17), (5, 16), (6, 17)
    )).inOrder
  def tick1 =
    tick(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 16), (5, 16), (6, 16), (5, 17)
    )).inOrder
  def tick2 =
    Function.chain(Nil padTo (18, tick))(s1).
    blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 0), (5, 0), (6, 0), (5, 1),
      (4, 17), (5, 17), (6, 17), (5, 18)
    )).inOrder
  def tick3 =
    Function.chain(Nil padTo (18, tick))(s3).
    blocks map {_.pos} must contain(exactly(
      (5, 0), (4, 17), (5, 17), (6, 17), (5, 18)
    )).inOrder
}
