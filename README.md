tetrix.scala
============

This is a falling block clone written in Scala. The programming process was documented daily on [eed3si9n.com](http://eed3si9n.com/).

The project consists of three modules.

### library

This the core module that implements the game logic. It also implements scripting test.

### swing

This module provides swing UI.


### tetrix_android

This module provides Android UI.

How to build
------------

Use sbt. To run swing UI:

```
> project swing
> run
```

To run scripting tests:

```
> project library
> run test
```

To install tetrix_android to your phone:

```
> project droid
> android:run
```

License
-------

MIT License.
