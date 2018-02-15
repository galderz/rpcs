# RPC Frameworks

## grpc

### hello world

```bash
cd grpc
mvn package exec:java -Dexec.mainClass=rpcs.grpc.hello.HelloServer
mvn package exec:java -Dexec.mainClass=rpcs.grpc.hello.HelloClient
```

# TODOs 

- [ ] Add tracing/telemetry
