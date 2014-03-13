# sbt-cppp

**sbt-cppp** (**Sbt** **C**ross-**P**roject **P**rotobuf **P**lugin) is a [Sbt](http://www.scala-sbt.org/) plugin to support [Protocol Buffers](http://code.google.com/p/protobuf/), especially in multi-project builds.

## Features

`sbt-cppp` compiles `*.proto` into `.java` files. In addition, `sbt-cppp` provides some features missed in [sbt-protobuf](https://github.com/sbt/sbt-protobuf) or other protobuf plugins:

* Jar packaging from `.proto` files.
* Cross-project `protoc` include path dependency management in multi-project builds.
* Cross-library `protoc` include path dependency management by auto-unzipping `.proto` files from jar packages.
* Support for custom code generator to `.proto` files.

## Usage

### Step 1: Install `sbt-cppp` into your project

Add the following line to your `project/plugins.sbt`:

    addSbtPlugin("com.dongxiguo" % "sbt-cppp" % "0.1.4")

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

 * `sbt-cppp` is for sbt 0.12 or 0.13
 * If `project-foo` depends on `project-bar`, `project-bar/src/protobuf/` will be added as a `protoc` include path when the plugin converts `project-foo/src/protobuf/*.proto` into `.java` files.
 * If you want to generate `.proto` files by some tools (instead of creating them manually), put `sourceGenerators in Protobuf += yourGenerator` in your `build.sbt`.
