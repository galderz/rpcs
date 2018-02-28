# RPC Frameworks

## grpc

### hello world

```bash
cd grpc
mvn package exec:java -Dexec.mainClass=rpcs.grpc.hello.HelloServer
mvn package exec:java -Dexec.mainClass=rpcs.grpc.hello.HelloClient
```

### infinispan

```bash
cd grpc
mvn package exec:java -Dexec.mainClass=rpcs.grpc.infinispan.InfinispanServer
mvn package exec:java -Dexec.mainClass=rpcs.grpc.infinispan.InfinispanClient
```

## aeron

### hello world

Run `io.aeron.samples.LowLatencyMediaDriver`

Run `rpcs.aeron.hello.BasicPublisher`

Run `rpcs.aeron.hello.BasicSubscriber`

Run `io.aeron.samples.AeronStat` 


# TODOs 

- [ ] Add tracing/telemetry
