package kafka.basics;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithKeys {
    public static void main(String[] args) {
        // System.out.println("Hello world!");

        Logger logger = LoggerFactory.getLogger(ProducerDemoWithKeys.class);

        // Create Producer properties

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // Crete the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        // Create a ProducerRecord
        for (int i = 0; i < 10; i++) {
            String topic = "first_topic";
            String value = "Hello World! " + Integer.toString(i);
            String key = "id_" + Integer.toString(i);

            ProducerRecord<String, String> producerRecord =
                    new ProducerRecord<String, String>(topic, key, value);
            // Send data - async
            producer.send(producerRecord, (RecordMetadata recordMetadata, Exception e) -> {
                if (e == null) {
                    logger.info("Received new Metadata \n" +
                            "Topic " + recordMetadata.topic() + "\n" +
                            "Patrition " + recordMetadata.partition() + "\n" +
                            "Offset " + recordMetadata.offset() + "\n" +
                            "Timestamp " + recordMetadata.timestamp() + "\n");
                } else {
                    logger.error("Error while producing ", e);
                }
            });
        }
        producer.flush();
        producer.close();
    }
}
