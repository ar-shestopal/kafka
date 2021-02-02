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

## Twitter project

 ----------      -------      ---------       ---------------
| Producer | -> | Kafka | <- | Consumer | -> | Elasticsearch |
 ----------      -------      ---------       ---------------

Code can be dound in `FirstProject/src/main/java/com/github/arshestopal/kafka/twitter`
Kafka Producer uses Twitter Client to read stream mof data from Twitter API, pushes data to kafka broker, which are then read by Consumer and are written to Elasticsearch Database.

Code is quite self explanatory.
Indempotent procucer is worth to mention.
Indempotent means that it dous not create duplicates. To make it work just set
`enable. idempotence=true`

It is the same as set
```
aks=1
retries= INTEGER_MAX_VALUE (may be different in different systems)
max.in.flight.requests.per.connection = 5 // For kafka version >= 2.0.0. For kafka version <= 1.1 use 1
 ```



 ### Elasticsearch

 Install it with (MacOS)
 ```
 brew install elasticsearch
 ```
open `/usr/local/etc/elasticsearch/elasticsearch.yml`
end change settings to
```
node.name: node-1
cluster.initial_master_nodes: ["node-1"] # as we want to have only 1 node in the cluster
```
Run
```
curl -XGET 127.0.0.1:9200
```
The response will be
```
{
  "name" : "node-1",
  "cluster_name" : "elasticsearch_oshestopal",
  "cluster_uuid" : "ICK54DpQRyW6FTRXNasp5Q",
  "version" : {
    "number" : "7.10.2",
    "build_flavor" : "default",
    "build_type" : "tar",
    "build_hash" : "747e1cc71def077253878a59143c1f785afa92b9",
    "build_date" : "2021-01-13T00:42:12.435326Z",
    "build_snapshot" : false,
    "lucene_version" : "8.7.0",
    "minimum_wire_compatibility_version" : "6.8.0",
    "minimum_index_compatibility_version" : "6.0.0-beta1"
  },
  "tagline" : "You Know, for Search"
}
```
Download schema for shekspir dataset , and dataset itself and use the schema
```
wget http://media.sundog-soft.com/es7/shakes-mapping.json
curl -H "Content-type: application/json" -XPUT 127.0.0.1:9200/shakespeare --data-binary @shakes-mapping.json
wget http://media.sundog-soft.com/es7/shakespeare_7.0.json
curl "Content-type: application/json" -XPOST '127.0.0.1:9200/shakespeare/_bulk' --data-binary @shakespeare_7.0.json
```

I willl ommit `Content-type: applilcation/json` just add it to each, curl command or use `ealsticsearch-head` plugin

Create new `movies` index
curl -XPUT localhost:9200/movies
```
{
  "mappings": {
    "properties": {
      "year": {
        "type": "date"
      }
    }
  }
}

Add individual item
```
curl -XPOST localhost:9200/movies/_doc/1
{
  "genre": [
    "SI-FI"
  ],
  "title": "Terminator",
  "year": 1986
}
```
