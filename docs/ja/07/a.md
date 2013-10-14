---
out: its-alive.html
---

### 動いた!

swing UI を起動すると、エージェントがゲームを乗っ取って tetrix を解きはじめる!:

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

![day7](../files/tetrix-in-scala-day7.png)

頭が悪すぎる!

探索木が浅すぎるため効用関数の違いが出てくる所まで到達していないからだ。デフォルトで `MoveLeft` を選択している。最初のオプションは探索木を深くしてより多くの動作をみることだ。いずれは取り組む必要があるけど、結局は問題全般の解決にはならない。第一に、ノード数は指数関数的に増えることを思い出してほしい。第二に、確実に分かっているピースが 2つしかないという問題がある。
