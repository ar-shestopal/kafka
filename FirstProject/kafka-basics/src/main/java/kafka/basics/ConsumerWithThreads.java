package kafka.basics;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerWithThreads {
    public static void main(String[] args) {
        new ConsumerWithThreads().run();
    }

    private ConsumerWithThreads() { }

    private void run() {
        String groupId = "my-fourth-application";
        String topic = "first_topic";
        String bootstrapServers = "127.0.0.1:9092";

        Logger logger = LoggerFactory.getLogger(ConsumerWithThreads.class.getName());

        // Latch for dealing with multiple threads
        CountDownLatch latch = new CountDownLatch(1);

        // Create the consumer runnable
        logger.info("Creating the consumer thread");
        Runnable consumerRunnable = new ConsumerRunnable(logger, topic,bootstrapServers,groupId, latch);

        // Start the thread
        Thread consumerThread = new Thread(consumerRunnable);
        consumerThread.start();

        // Create shutdown hook
        Runtime.getRuntime().addShutdownHook( new Thread( () -> {
            logger.info("Caught shutdown hook");
            ((ConsumerRunnable) consumerRunnable).shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Application has exited!");
        }));

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Application was interrupted!");
        } finally {
            logger.info("Application is closing");
        }


    }

    private class ConsumerRunnable implements Runnable {
        private final Logger logger;
        private final CountDownLatch latch;
        private final String topic;
        private final KafkaConsumer<String, String> consumer;

        public ConsumerRunnable(Logger logger, String topic, String bootstrapServers, String groupId, CountDownLatch latch) {
            this.latch = latch;
            this.topic = topic;
            this.logger = logger;

            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            consumer = new KafkaConsumer<String, String>(properties);

            consumer.subscribe(Arrays.asList(topic));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("Key: " + record.key() + ", Value: " + record.value());
                        logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                    }
                }
            } catch (WakeupException e) {
                logger.info("Received shutdown signal!");
            } finally {
                consumer.close();
                latch.countDown();
            }
        }

        public void shutdown() throws WakeupException {
            // Special method to interrupt consumer.poll()
            consumer.wakeup();
        }
    }
}
