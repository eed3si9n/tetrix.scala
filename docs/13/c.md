---
out: survival-of-the-fittest.html
---

### survival of the fittest

We've tuned the penalty parameters yesterday to increase attack counts, but it still feels conservative.

```
h11:c10:k1 = lines  : Vector(34, 34, 32, 38, 29) // 34 +/- 5
h11:c10:k1 = attacks: Vector(4, 3, 3, 5, 1)      // 3 +/- 2
```

I wonder if rid of the reward for deleting a single line would improve this.

```scala
  def reward(s: GameState): Double =
    if (s.lastDeleted < 2) 0
    else s.lastDeleted
```

Here are the results:

```
h11:c10:k1:s0 = lines  : Vector(25, 34, 24, 38, 39) // 34 +/- 5
h11:c10:k1:s0 = attacks: Vector(2, 3, 1, 5, 0)      // 2 +/- 3
```

The minimum value of the line count decreased to 24, and the minimum value of the attack count didn't change. Because it's not constantly clearing out single lines, it probably became less stable player.

### cavity again

To bring in the structure, we try making the cavity stronger again.

```
h11:c10:w1:s0 = lines  : Vector(39, 24, 20, 51, 34) // 34 +/- 17
h11:c10:w1:s0 = attacks: Vector(2, 1, 1, 7, 1)      // 1 +/- 6
```

Interestingly, the median of the attack count went down to 1, but the max of line count increased to 51. Here's the result from 1.txt:

```
    xx    
    xxx   
   xxx    
   xxx x  
   xxxxxx 
xx xxxxxx 
x xxxxxxxx
xxxxx xxx 
xxxxxxxxx 
xxxxxxxxx 
xxxxxxxxx 
xxxxxxxxx 
xxxxxxxxx 
xxxxxxxxx 
xxxxxxxxx 
xxxxxxxxx 
xxxxxxx x 
xxxxxxxxx 
 xxxxxxxxx
xxxxxxxxx 
xxxxxxxxx 
 xxxxxxxxx
----------
```

### crevasse depth

Next, I decided to penalize crevasses only if it's deeper than 3 to increase the opportunity of attacks.

```
h11:c'10:w1:s0 = lines  : Vector(39, 39, 18, 52, 25) // 39 +/- 21
h11:c'10:w1:s0 = attacks: Vector(3, 5, 2, 6, 0)      // 3 +/- 3
```

This has stablized the game a bit. The Android version is playable, but swing version with this aggressive tuneup is getting furious.

![day13](http://eed3si9n.com/images/tetrix-in-scala-day13.png)

The agent has more line count racked up.

### thanks!

Anyway, this is going to be the end of our tetrix in Scala series. Thanks for the comments and retweets. I'd like to hear what you think. Also, if you are up for the challenge, send me a pull request of a smarter agent-actor that can beat human!
