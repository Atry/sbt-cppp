# sbt-cppp

**sbt-cppp** (Sbt **C**ross-**P**roject **P**rotobuf **P**lugin) is a [Sbt](http://www.scala-sbt.org/) plugin to support [Protocol Buffers](http://code.google.com/p/protobuf/) in multi-project builds.

## Usage

### Step 1: Install `sbt-cppp` into your project

Add the following line to your `project/plugins.sbt`:

    addSbtPlugin("com.dongxiguo" % "sbt-cppp" % "0.1.2")

And add `protobufSettings` and `protobuf-java` dependency to your `build.sbt`:

    protobufSettings
    
    libraryDependencies += "com.google.protobuf" % "protobuf-java" % "2.5.0"

### Step 2: Install `protoc` into `$PATH`

For windows, download at http://code.google.com/p/protobuf/downloads/detail?name=protoc-2.5.0-win32.zip. For most linux distributions, looking for `protobuf-compiler` package.

### Step 3: Create your `.proto` files.

Create `src/protobuf/sample_proto.proto`

    message SampleMessage {
      optional int32 sample_field = 1;
    }

### Step 4: Use the `.proro` files in your source files.

Create `src/main/scala/SampleMain.scala`:

    object SampleMain {
      def main(args: Array[String]) {
        println(SampleMessage.newBuilder.setSampleField(123).build)
      }
    }

### Step 5: Run it!

    $ sbt
    > run-main SampleMain

## Further information

 * `sbt-cpp` is for sbt 0.12 or 0.13
 * If `project-foo` depends on `project-bar`, `project-bar/src/protobuf/` will be added into `protoc` include path when the plugin converts `project-foo/src/protobuf/*.proto` into `.java` files.
 * If you want to generate `.proto` files by some tools (instead of creating them manually), put `sourceGenerators in Protobuf += yourGenerator` into your `build.sbt`.
