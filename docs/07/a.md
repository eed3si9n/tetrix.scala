---
out: its-alive.html
---

### it's alive!

Running the swing UI, the agent actually takes over the game and starts solving tetrix!:

```
[info] Running com.tetrix.swing.Main 
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
[info] selected MoveLeft
...
```

![day7](files/tetrix-in-scala-day7.png)

And it's so dumb!

Because the search tree is too shallow it never actually reach a point where utility actually kicks in. By default it's picking `MoveLeft`. The first option is to deepen the search tree to more moves. We need eventually need that, but ultimately it's not going to solve the entire problem. First, remember, the number of nodes grows exponentially. Second, we only know about two pieces for sure.
