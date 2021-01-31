package com.github.arshestopal.kafka.twiter;

import com.github.arshestopal.kafka.first.ConsumerWithThreads;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {
    private String consumerKey;
    private String consumerSecret;
    private String token;
    private String secret;
    private final Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());
    private String bootstrapServers;
    private String topic;

    public TwitterProducer() {
        Dotenv dotenv = Dotenv.load();

        this.consumerKey = dotenv.get("TWITTER_CONSUMER_KEY");
        this.consumerSecret = dotenv.get("TWITTER_CONSUMER_SECRET");
        this.secret = dotenv.get("TWITTER_SECRET");
        this.token = dotenv.get("TWITTER_TOKEN");
        this.bootstrapServers = dotenv.get("BOOTSTRAP_SERVER", "localhost:9092");
        this.topic = dotenv.get("TOPIC", "twitter_tweets");

    }

    public void run() {
        // create a client
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

        Client client = createTwitterClient(msgQueue);

        // Attempts to establish a connection.
        client.connect();

        // create a producer
        KafkaProducer<String, String> producer = createKafkaProducer();

        // create shutdown hook
        Runtime.getRuntime().addShutdownHook( new Thread( () -> {
            logger.info("Stopping application...");
            client.stop();
            producer.close();
            logger.info("Application has exited!");
        }));

        // loop to send data to kafka

        // something smarter should be used like ScheduleExecutorService
        // but for simplicity, do a while loop.
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }

            if (msg != null){
                ProducerRecord<String,String> producerRecord =
                        new ProducerRecord<String, String>(topic,null, msg);
                // Send data - async
                producer.send(producerRecord, (RecordMetadata recordMetadata, Exception e) -> {
                    if (e != null) logger.error("Error while producing", e);
                });
                producer.flush();
                producer.close();
            }
        }
    }

    private Client createTwitterClient(BlockingQueue<String> msgQueue) {
        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // Optional: set up some followings and track terms
        List<String> terms = Lists.newArrayList("bitcoin");
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-Twitter-Producer")  // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));


        Client hosebirdClient = builder.build();

        return hosebirdClient;
    }

    private KafkaProducer<String, String> createKafkaProducer() {

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Crete the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        return producer;
    }

    public static void main(String[] args) {
        new TwitterProducer().run();
    }
}
