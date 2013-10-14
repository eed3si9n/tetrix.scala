---
out: aima.html
---

  [russell]: http://aima.cs.berkeley.edu/
  [amazon2]: http://www.amazon.co.jp/dp/4320122151

### Russell と Norvig

大学で計算機科学を専攻に選んだ理由の一つが AI について習うことだった。しかし、実際始まってみると最初の数年間の講義では一切 AI のようなものが出て来なかったのでかなりガッカリした。そこである夏の産学連携 (co-op) インターンシップのときに早起きしてスターバックスに行き、頭の良さそうな大学で AI を教えるのに使っている教科書を読んでみることに決めた。そうして見つけたのが Russell と Norvig の [Artificial Intelligence: A Modern Approach (AIMA)][russell]だ (邦訳は[エージェントアプローチ人工知能 第2版][amazon2])。

衝撃的な本だった。人間のようなロボットを作ろうとするのではなく、合理的に**行動**するエージェントという概念を導入した。

> **エージェント**とは、センサを用いてその環境を認識し、アクチュエータを用いて環境に対して行動を取ることができる全てのものだ。

合理的エージェントの構造の一つにモデルベース、効用ベースエージェントというものがある。

```
+-エージェント--------------+   +-環境-+ 
|           センサ        <=====     |
|     状態 <----+          |   |     |
|              |           |   |     |
|   アクションAを実行すると   |   |     |
|   どうなるだろう?         |    |     |
|              |          |   |     |
| どれだけ幸せになれるだろう? |   |     |
|              |          |   |     |
|     効用 <----+          |   |     |
|              |          |   |     |
|  次に何をするべきだろう?   |   |     |
|              |          |   |     |
|        アクチュエータ      =====>   |
+-------------------------+   +-----+
```

> 効用関数 (utility function) は状態 (もしくは一連の状態の列) を関連する幸せさの度合いを表す実数に投射する。

ガツンと来ない? この構造によれば知的にみえるプログラムを構築するのに必要なものはステートマシン (できた!)、効用関数、それから木探索アルゴリズムだけだ。結局データ構造やグラフ理論の講義が役に立つということだ。

### 効用関数

効用ベースのエージェントの場合は、効用関数の作り方が鍵となる。多分今後いじっていく事になるけどまずはシンプルなものから始めよう。今のところは、幸せさは死んでいないことと、消したラインだと定義する。消極的に聞こえるかもしれないけど、tetrix は負けない事を競うゲームだ。1対1 の tetrix では明確な勝利の定義は無い。対戦相手が負けることでデフォルトとして勝者が決まる。

新しいスペックを作ってこれを記述してみよう:

```scala
import org.specs2._

class AgentSpec extends Specification with StateExample { def is =            s2"""
  This is a specification to check Agent

  Utility function should
    evaluate initial state as 0.0,                                            \$utility1
    evaluate GameOver as -1000.0,                                             \$utility2
                                                                              """
  
  import com.eed3si9n.tetrix._
  import Stage._

  val agent = new Agent

  def utility1 =
    agent.utility(s1) must_== 0.0 
  def utility2 =
    agent.utility(gameOverState) must_== -1000.0
}
```

次に `Agent` クラスを始めて `utility` メソッドをスタブする:

```scala
package com.eed3si9n.tetrix

class Agent {
  def utility(state: GameState): Double = 0.0
}
```

期待通り 2つ目の例で失敗する:

```
[info] Utility function should
[info] + evaluate initial state as 0.0,
[error] x evaluate GameOver as -1000.0.
[error]    '0.0' is not equal to '-1000.0' (AgentSpec.scala:8)
```

直そう:

```scala
  def utility(state: GameState): Double =
    if (state.status == GameOver) -1000.0
    else 0.0
```

オールグリーン。特にリファクタリングするものも無い。
