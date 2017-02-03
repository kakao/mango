Mango
=====

[![Build Status](https://travis-ci.org/kakao/mango.svg?branch=master)](https://travis-ci.org/kakao/mango) [![Join the chat at https://gitter.im/kakao/mango](https://badges.gitter.im/kakao/mango.svg)](https://gitter.im/kakao/mango?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Mango is a core utility library written Scala for handing JSON formats, concurrency, logging, hashing, cryptography, reflections, etc., as well as making client connections to various endpoints, including HTTP, Telnet, OpenTSDB, Couchbase, ElasticSearch, HBase and ZooKeeper. 
 
Mango has been extensively used in Kakao for years, and is open-sourced under Apache License. Its name follows [Googlers' wisdom](https://github.com/google/guava) that core libraries should be named after tropical fruits. 

Mango is composed of several subprojects, and its dependency structure is shown below, where the upper one is dependent on the lower.

```
                         ┌───────────┐
                         │   mango   │
                         └─────┬─────┘
                 ┌─────────────┼────────────────┐
           ┌─────┴─────┐   ┌───┴───┐   ┌────────┴──────┐
           │ couchbase │   │ hbase │   │ elasticsearch │
           └─────┬─────┘   └───┬───┘   └────────┬──────┘ 
                 │          ┌──┴───┐            │
                 │          |  zk  │            │
                 │          └──┬───┘            │
                 └─────────────┼────────────────┘
                          ┌────┴─────┐
                          │   core   │
                          └────┬─────┘
                         ┌─────┴─────┐
                         │   macro   │
                         └───────────┘
``` 

For example, you can pull the entire Mango library or import only the core and logging features, as follows:

<!-- DO NOT EDIT: The section below will be automatically updated by build script -->
```scala
libraryDependencies += "com.kakao.mango" %% "mango" % "0.5.0-SNAPSHOT"
libraryDependencies += "com.kakao.mango" %% "mango-core" % "0.5.0-SNAPSHOT"
```
<!-- DO NOT EDIT: The section above will be automatically updated by build script -->

Some examples of using Mango are shown below; more comprehensive documentations are in the wiki.

## [JSON Conversions](https://github.com/kakao/mango/wiki/JSON)

It is simple to parse and serialize JSON files.

```scala
import com.kakao.mango.json._

// serialize a case class or Map to JSON
val data = Map("happy" -> "new year")
val json = toJson(data)        // json: String = {"happy":"new year"}

// parse json string into Map[String, Any]
val str = """{"happy":"new year"}"""
val parsed = parseJson(str)   // parsed: Map[String, Any] = Map(happy -> new year)

// parse json string into a case class
case class Foo(happy: String)
val foo = fromJson[Foo](json)  // foo: Foo = Foo(new year)
```

## [HTTP Requests](https://github.com/kakao/mango/wiki/HTTP)

Make asynchronous HTTP requests simply.

```scala
import com.kakao.mango.http._
import com.kakao.mango.concurrent._

val future = Get("http://example.com/")
val response = future.sync()
println(response.body) // "<!doctype html>\n<html> ..."
```

## [Logging](https://github.com/kakao/mango/wiki/Logging)

Mango logging provides a wrapper of [SLF4j](http://www.slf4j.org/) for easier use with Scala. By extending `Logging`, `logger` field is available for logging. It also has a quirk to print the source line numbers, using SLF4j markers.  

```scala
import com.kakao.mango.logging.Logging

class AwesomeClass extends AwesomeBaseClass with Logging {
  logger.info(s"quick brown fox blah blah")
}
```

# Development

### Shaded Dependencies

This project contains shaded version of some libraries that are prone to version conflicts in big projects. They are currently: Jackson (with Scala module), Guava, Netty 3.x, Asynchbase, and AsyncHttpServer. It uses some SBT trick to relocate the classes to `com.kakao.shaded`.

* `mango-shaded-lib` contains the Java dependencies whose classes are relocated using sbt-assembly plugin. The shaded assembly is added as an unmanaged dependency of the `mango-shaded` subproject.
* `mango-shaded` contains manually relocated Scala files, which cannot be automatically relocated by sbt-assembly.

Because of the unmanaged dependency, it may be necessary to run the following command before opening this project in an IDE.

    sbt "+ compile"
    

### Cross Compilation and Publishing

To be compatible with both JRE 1.7 and 1.8, it is required by the build script to use JDK 1.7 while building this project. Set `JAVA_HOME` accordingly before setting up an IDE or running SBT commands.

Since Mango supports both Scala 2.10 and 2.11, it must be cross-complied when publishing:

```
# Publishing snapshots
sbt "+ publish"

# Publishing releases 
sbt "release cross"
```

When publishing snapshots, the `+` is prepended in the `publish` command to make SBT to use the `crossScalaVersions` setting, whereas `release cross` should be used for publishing releases so that `sbt-release-plugin` can handle the release cycle.



## License

This software is licensed under the [Apache 2 license](LICENSE.txt), quoted below.

Copyright 2017 Kakao Corp. <http://www.kakaocorp.com>

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
