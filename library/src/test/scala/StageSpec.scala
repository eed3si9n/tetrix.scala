import org.specs2._

class StageSpec extends Specification with StateExample { def is =            s2"""
  This is a specification to check Stage

  The current piece should
    be initialized to the first element in the state.                         $init1

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

  Dropping the current piece should
    tick the piece until it hits something.                                   $drop1

  Spawning a new piece should
    end the game it hits something.                                           $spawn1

  Deleting a full row should
    increment the line count.                                                 $line1

  Attacks should
    increment the pending attack count,                                       $attack1
    and change the blocks in the view on spawn.                               $attack2
                                                                              """

  import com.eed3si9n.tetrix._
  import Stage._

  def init1 =
    (s4.currentPiece.kind must_== OKind) and
    (s4.blocks map {_.pos} must contain(exactly(
      (4, 18), (5, 18), (4, 17), (5, 17)
    )).inOrder)
  def left1 =
    moveLeft(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (3, 18), (4, 18), (5, 18), (4, 19)
    )).inOrder
  def leftWall1 =
    Function.chain(moveLeft :: moveLeft :: moveLeft ::
        moveLeft :: moveLeft :: Nil)(s1).
      blocks map {_.pos} must contain(exactly(
      (0, 0), (0, 18), (1, 18), (2, 18), (1, 19)
    )).inOrder
  def leftHit1 =
    moveLeft(s2).blocks map {_.pos} must contain(exactly(
      (3, 18), (4, 18), (5, 18), (6, 18), (5, 19)
    )).inOrder
  def right1 =
    moveRight(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (5, 18), (6, 18), (7, 18), (6, 19)
    )).inOrder
  def rotate1 =
    rotateCW(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (5, 19), (5, 18), (5, 17), (6, 18)
    )).inOrder
  def tick1 =
    tick(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 17), (5, 17), (6, 17), (5, 18)
    )).inOrder
  def tick2 =
    Function.chain(Nil padTo (19, tick))(s1).
    blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 0), (5, 0), (6, 0), (5, 1),
      (4, 18), (5, 18), (6, 18), (5, 19)
    )).inOrder
  def tick3 =
    Function.chain(Nil padTo (19, tick))(s3).
    blocks map {_.pos} must contain(exactly(
      (5, 0), (4, 18), (5, 18), (6, 18), (5, 19)
    )).inOrder
  def drop1 =
    drop(s1).blocks map {_.pos} must contain(exactly(
      (0, 0), (4, 0), (5, 0), (6, 0), (5, 1),
      (4, 18), (5, 18), (6, 18), (5, 19)
    )).inOrder
  def spawn1 =
    Function.chain(Nil padTo (10, drop))(s1).status must_==
    GameOver
  def line1 =
    (s3.lineCount must_== 0) and
    (Function.chain(Nil padTo (19, tick))(s3).
    lineCount must_== 1) and
    (Function.chain(Nil padTo (19, tick))(s3).
    lastDeleted must_== 1)
  def attack1 =
    notifyAttack(s1).pendingAttacks must_== 1
  def attack2 =
    Function.chain(notifyAttack :: drop :: Nil)(s1).blocks map {_.pos} must contain(
      (0, 1), (4, 1), (5, 1), (6, 1), (5, 2),
      (4, 18), (5, 18), (6, 18), (5, 19)
    )
}
