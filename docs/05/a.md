---
out: grid-size.html
---

### size of the grid

After playing a few times, I noticed that the effective size of the grid is much smaller than 10x20 because how low the spawning point is. To work around this, we should expand the grid size vertically, but display only the lower 20 rows at least for the swing UI. I'll keep the specs to be 10x20 so I don't have to change all the numbers. `newState` should accept gridSize`:

```scala
  def newState(blocks: Seq[Block], gridSize: (Int, Int),
      kinds: Seq[PieceKind]): GameState = ...
```

Now, mostly the change is at swing UI. Pass in chopped off `gridSize` for rendering:

```scala
    drawBoard(g, (0, 0), (10, 20), view.blocks, view.current)
    drawBoard(g, (12 * (blockSize + blockMargin), 0),
      view.miniGridSize, view.next, Nil)
```

Next, filter to only the blocks within the range:

```scala
    def drawBlocks {
      g setColor bluishEvenLigher
      blocks filter {_.pos._2 < gridSize._2} foreach { b =>
        g fill buildRect(b.pos) }
    }
    def drawCurrent {
      g setColor bluishSilver
      current filter {_.pos._2 < gridSize._2} foreach { b =>
        g fill buildRect(b.pos) }
    }
```

Now the newly spawned piece creeps from the top edge of the grid.

![day5](files/tetrix-in-scala-day5.png)

As always, the code's up on github:

```
\$ git fetch origin
\$ git co day5v2 -b try/day5
\$ sbt swing/run
```
