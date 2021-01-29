## This is a short reference on kafka setup and cli

## Kafka systemdescription
Kafka is kdistributed messagikng systemthat can work as both messege queue and Pub/Sub system

### Brokers
We can run multiple brockers, connected with one another into a cluster. Itmeans thay have replication system and synchronization.

Brockers are managed by `zookeeper`server.

### Topics and partitions
We send data into topics.
Each topic has at leastone partition.
Partition is the way tosplit the data.
If in the message we specify the `key` the message with the same key will always go to the same partition.

We can also think about partition as about the separate `<queue>` data structure it which we stor actual data.

Each partition has an `offset`, current position, from which we can read the data.

#### Replication

Partiotion is replicated throughout available brockers, and each brocker can be a leader for one partiotion, but the folower for other partitions. Leakers election is also managed by `zookeepre`

### Producers, Consumers and ConsumerGroups

`Producer` is a tool that pushes the data into certain topic and certain pertion.

`Consumer` can read data from some topic, but from multiple partitions inside that topics.

`ConsumerGroup` can have multiple consumers. But waht is important is that `inside one group, each consumer can read data from one pertition`.
It means that if we put all Consumers into one ConsumerGroup, each consumer in the group will read only fro one partition, and each message in the topic will consumed by `one` Consumer only. It means that we effectively turn kafka into a message queue where each message is read `only once`.

On the other hand if we assing each Consumenr to its own ConsumerGroup, it will be able to read for all partitons, which turns kafka into Pub/Sub sustem/

## Setup with brew
```
brew install kafka
```
or download from
`https://www.apache.org/dyn/closer.cgi?path=/kafka/2.7.0/kafka_2.13-2.7.0.tgz`
and run
```
$ tar -xzf kafka_2.13-2.7.0.tgz
$ cd kafka_2.13-2.7.0
```

Change `config/server.properties`
```
log.dirs=/ABSOLUTE_PATH_TO_DIR/kafka_2.13-2.7.0/data/kafka
```
Change `config/zookeeper.properties`
```
dataDir=/ABSOLUTE_PATH_TO_DIR/kafka_2.13-2.7.0/data/zookeeper
```

Start zookeeper
```
zookeeper-server-start config/zookeeper.properties
```
Start kafka broker
```
zookeeper-server-start config/zookeeper.properties
```

##CLI
### Topics
Create topic
```
kafka-topics --zookeeper 127.0.0.1:2181 --topic first_topic --create --partitions 3 --replication-factor 1
```
List topics
```
kafka-topics --zookeeper 127.0.0.1:2181 --list
```
Describe
```
kafka-topics --zookeeper 127.0.0.1:2181 --topic first_topic --describe
```
Delete
```
kafka-topics --zookeeper 127.0.0.1:2181 --topic first_topic --delete
```
It will mark topic as deleted and delete only if delete.topic.enable is set to true (true by default)

### Console Producer
Producer created the topic if it is not present but with default parameters
for example `num.partitions` taken from server.properties

```
kafka-console-producer --bootstrap-server 127.0.0.1:9092 --topic first_topic producer-property acks=all
```
When the acks property is set to all, you can achieve exactly once delivery semantics. The Kafka producer sends the record to the broker and waits for a response from the broker. If no acknowledgment is received for the message sent, then the producer will retry sending the messages based on the retry config being set to n. The broker sends acknowledgment only after replication based on the min.insync.replica property.

For example, a topic may have a replication factor of 3 and a min.insync.replica of 2. In this case, an acknowledgment will be sent after the second replication is complete. In order to achieve exactly once delivery semantics the broker has to be idempotent. Acks = all should be used in conjunction with min.insync.replicas.

Other values may be `0` of `1` but thay can cause data loss.

### Console Consumer

```
kafka-console-consumer --bootstrap-server 127.0.0.1:9092 --topic first_topic
```

Reads all new messages from a topic

## Java applications

In `FirstProject/src/main/java/com/github/arshestopal/kafka/first`

are simple the Producer and Consumer clases with different basic features, the goal is to demonstrate how backbone functionality canbe created programatically.