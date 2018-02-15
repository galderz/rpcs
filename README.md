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

# TODOs 

- [ ] Add tracing/telemetry
