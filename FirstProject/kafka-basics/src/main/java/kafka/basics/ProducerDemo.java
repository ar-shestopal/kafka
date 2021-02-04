package kafka.basics;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducerDemo {
    public static void main(String[] args) {
        // System.out.println("Hello world!");

        // Create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        // Crete the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        // Create a ProducerRecord

        ProducerRecord<String,String> producerRecord =
                new ProducerRecord<String, String>("first_topic", "Hello World!");
        // Send data - async
        producer.send(producerRecord);
        producer.flush();
        producer.close();
    }
}
