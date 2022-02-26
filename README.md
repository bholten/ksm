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
  // That'streams it!
}
```

This single object is an entire, production-ready application, with a
`/health` endpoint, configuration, and so on. No fancy annotations,
multiple interfaces to implement -- just a `Topology` value.

## Example Applications ##

See the `examples/` directory.

## FAQ ##

Q: I want to customize the http4s endpoint.

A: Overwrite the `routes` value when implementing `Streamlet`.

The `routes` value is of type `KafkaRoutes[F[_]]` and is intended to
provide a health check; however, you can extend it to provide access
to a state store or whatever else.

The trait is very simple:

``` scala
trait KafkaRoutes[F[_]] {
  def routes(handler: StreamHandler[F]): HttpRoutes[F]
}
```

Q: I want to customize the Kafka Streams startup handler.

A: Overwrite the `beforeStart` value when implementing `Streamlet`.

The `beforeStart` value is a `StartupHandler` that runs before the
`KafkaStreams` object is started. The default is `KafkaFailFast`,
which sets the uncaught exception handler to `SHUTDOWN_APPLICATION`.

The `StartupHandler` trait is also very simple:

``` scala
trait StartupHandler {
  def beforeStart(kafkaStreams: KafkaStreams): KafkaStreams
}
```

## Roadmap ##

Biggest thing now is to improve testing. I pretty much re-wrote
everything from Cats Effect 3, and the tests are a major
work-in-progress and frankly the only thing holding me back from
publishing artifacts.

There is some effort to "Catsify" this more and make it easier to
integrate into larger, Typelevel-style applications.

The initial design was highly pragmatic and open sourced from a
client; however, this should make it more extensible. That said, I do
wish to keep the Typelevel ecosystem relatively opaque for users just
implementing topologies.

- More mixins, e.g. Prometheus
- More configurable http4s
- More endpoints, e.g. resume failed tasks, querable state stores

## Contribution policy ##

Contributions via GitHub pull requests are gladly accepted from their
original author. Along with any pull requests, please state that the
contribution is your original work and that you license the work to
the project under the project'streams open source license. Whether or not
you state this explicitly, by submitting any copyrighted material via
pull request, email, or other means you agree to license the material
under the project'streams open source license and warrant that you have the
legal authority to do so.

## License ##

This code is open source software licensed under the
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0) license.
