---
out: survival-of-the-fittest.html
---

### 適者生存

昨日、攻撃数を上げるためにペナルティのパラメータを調整したけど、まだ少しおとなし目な気がする。


```
h11:c10:k1 = lines  : Vector(34, 34, 32, 38, 29) // 34 +/- 5
h11:c10:k1 = attacks: Vector(4, 3, 3, 5, 1)      // 3 +/- 2
```

一行しか消去しなかったときの賞与を無くせば改善するんじゃないだろうか。

```scala
  def reward(s: GameState): Double =
    if (s.lastDeleted < 2) 0
    else s.lastDeleted
```

これが結果だ:

```
h11:c10:k1:s0 = lines  : Vector(25, 34, 24, 38, 39) // 34 +/- 5
h11:c10:k1:s0 = attacks: Vector(2, 3, 1, 5, 0)      // 2 +/- 3
```

ライン数の最小値は 24 に減少、攻撃数の最小値は変わらなかった。常にラインを消し続けているタイプのプレーヤーではなくなったので、不安定になった感じだ。

### 虫歯、再び

厳しさを増すために、虫歯のペナルティーを再び増やしてみる。

```
h11:c10:w1:s0 = lines  : Vector(39, 24, 20, 51, 34) // 34 +/- 17
h11:c10:w1:s0 = attacks: Vector(2, 1, 1, 7, 1)      // 1 +/- 6
```

興味深いことに攻撃数の中間値は 1 まで減少したけど、ライン数の最大値は 51 にもなった。1.txt の結果はこうなっている:

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

### クレバスの深さ

次に、攻撃のチャンスを作るために、クレバスの深さが 4 以上じゃなければ罰しないことにした。

```
h11:c'10:w1:s0 = lines  : Vector(39, 39, 18, 52, 25) // 39 +/- 21
h11:c'10:w1:s0 = attacks: Vector(3, 5, 2, 6, 0)      // 3 +/- 3
```

これでゲームが少し安定してきた。Android 版はそこそこ遊べるようになった。一方 swing は今回のアグレッシブな調整を経てアツくなってきてる。

![day13](../files/tetrix-in-scala-day13.png)

ライン数だけ見るとエージェントの方が優っている。

### 謝辞

さて、Scala で書く tetrix もこれで最終回だ。コメントやリツイートありがとう。意見や至らない所があれば是非聞かせてほしい。それから、腕に自信がある人は人間を倒せるぐらい頭の良いエージェントアクターを pull request で送ってほしい!
