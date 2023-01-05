package me.myself.kk

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import java.util.function.Supplier
import net.datafaker.Faker
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.Grouped
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.Produced
import java.util.function.Consumer
import java.util.function.Function

@SpringBootApplication
class KkApplication {
	@Bean
	fun produceChuckNorris(): Supplier<Message<String>> {
		return Supplier {
			MessageBuilder.withPayload(Faker.instance().chuckNorris().fact()).build()
		}
	}

	@Bean
	fun consumeChuckNorris(): Consumer<Message<String>> {
		return Consumer {s: Message<String> ->
			println(
				"FACT:\u001B[3m: " + s.payload + "\u001B[0m"
			)
		}
	}

	@Bean
	fun processWords(): Function<KStream<String?, String>, KStream<String, Long>> {
		return Function { inputStream: KStream<String?, String> ->
			val countsStream = inputStream
				.flatMapValues { value: String -> value.lowercase().split("\\W+".toRegex()) }
				.map { _: String?, value: String -> KeyValue(value, value) }
				.groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
				.count(Materialized.`as`("word-count-state-store"))
				.toStream()
			countsStream.to("counts", Produced.with(Serdes.String(), Serdes.Long()))
			countsStream
		}
	}
}

fun main(args: Array<String>) {
	runApplication<KkApplication>(*args)
}
