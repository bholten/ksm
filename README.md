# ksm #

**ksm** is a Scala **K**afka **S**treams **m**icro-framework aimed at
making it as simple as possible to get up and running in Kafka Streams
and production-ready, particularly in containerized environments.

Out of the box, it provides:

- configuration management following 12-factor principles
- an HTTP /health endpoint
- a StatsD/DataDog metrics client

It is battle tested. We're running this on over 120+ applications in
production.

## tl;dr ##

You implement the `Streamlet` trait by overriding the
`Topology`. That's it. The topology is run and given to http4s in
order to provide a `/health` endpoint automatically. You can
optionally mixin a StatsD metrics trait.

Simple to medium complexity `ksm` applications are frequently a
single-file.

See the canonical `WordCount` example.

```scala
// Streamlet gives you a main entrypoint automatically
object WordCount extends Streamlet with LazyLogging {
  logger.info("Starting WordCount")
  private val config      = ConfigFactory.load()
  private val inputTopic  = config.getString("word-count.input.topic")
  private val outputTopic = config.getString("word-count.output.topic")

  // The only thing you need to override is the Topology
  override val topology: Topology = {
    // Scala gets extra help with implicits
    import org.apache.kafka.streams.scala.ImplicitConversions._
    import org.apache.kafka.streams.scala.Serdes._
    implicit val countStore: Materialized[String, String, InMemoryKeyValueStore] =
      Materialized.as("count-store")

    val builder = new StreamsBuilder()

    // Get the input stream
    builder
      .stream[String, String](inputTopic)
      // Split the words
      .flatMapValues(_.split("\\W+"))
      // Repartition on word
      .groupBy((_, word) => word)
      // Aggregate => (word, count)
      .count()
      // Output to KStream
      .toStream
      .to(outputTopic)

    builder.build()
  }
  // That's it!
}
```

This single object is an entire, production-ready application, with a
`/health` endpoint, configuration, and so on. No fancy annotations,
multiple interfaces to implement -- just a `Topology` value.

## Example Applications ##

See the `examples/` directory.

## Rationale ##

I've worked as a consultant on several dozen Kafka Streams projects,
and my experience both developing and teaching how to do event-driven
Kafka Streams architectures comes down to a few principles, which
we've attempted to address with **ksm**:

- Keep it as simple as possible
- Just use the plain Kafka Streams DSL and Processor APIs, don't try
  to invent your own DSL
- The stream-processing should only do stream-processing; no mixing
  other RPC
- One topology per application
- Follow 12-factor
- Use the container runtime (Kubernetes, OpenShift) to manage the
  health and lifecycle of the application; don't get fancy with
  supervision strategies, etc. when health checks and metrics will
  suffice
- Keep it as simple as possible!

That being said, there are situations where you'll need to break some
of these rules. For example, mixing RPC will be necessary if you need
to enrich data in Kafka with some external procedure call, for example
in a database or REST call. Or, you will want to expose some REST
endpoint for doing operations on the stream-processor while messages
are in-flight. **ksm** does not try to address these situations. It
does not try to be good at *everything*.

These situations are valid and necessary, however they will likely be
on the edges for your system. Furthermore, there are plenty of fine
frameworks that already work excellent in these situations -- for
Scala, my favorite is Akka.

## Roadmap ##

- More mixins, e.g. Prometheus
- More configurable http4s
- More endpoints, e.g. resume failed tasks, querable state stores

## I want a Java version ##

I don't have plans to support Java.

However, a Java framework with a similar philosophy (albeit much
heavier) is the excellent [Azkarra
Streams](https://www.azkarrastreams.io/)

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their
original author. Along with any pull requests, please state that the
contribution is your original work and that you license the work to
the project under the project's open source license. Whether or not
you state this explicitly, by submitting any copyrighted material via
pull request, email, or other means you agree to license the material
under the project's open source license and warrant that you have the
legal authority to do so.

## License ##

This code is open source software licensed under the
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license.
