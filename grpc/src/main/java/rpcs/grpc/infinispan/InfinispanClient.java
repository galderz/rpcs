package rpcs.grpc.infinispan;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import rpcs.grpc.hello.HelloClient;
import rpcs.grpc.infinispan.InfinispanOuterClass.Key;
import rpcs.grpc.infinispan.InfinispanOuterClass.KeyValue;
import rpcs.grpc.infinispan.InfinispanOuterClass.Value;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InfinispanClient {

   private static final Logger LOG = Logger.getLogger(HelloClient.class.getName());

   private final ManagedChannel channel;
   private final InfinispanGrpc.InfinispanBlockingStub blockingStub;

   public InfinispanClient(String host, int port) {
      this(ManagedChannelBuilder.forAddress(host, port)
         // Channels are secure by default (via SSL/TLS).
         // For the example we disable TLS to avoid needing certificates.
         .usePlaintext(true)
         .build());
   }

   InfinispanClient(ManagedChannel channel) {
      this.channel = channel;
      blockingStub = InfinispanGrpc.newBlockingStub(channel);
   }

   public void shutdown() throws InterruptedException {
      channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
   }

   public void put(byte[] key, byte[] value, String cacheName) {
      KeyValue req = KeyValue.newBuilder()
         .setCacheName(cacheName)
         .setKey(ByteString.copyFrom(key))
         .setValue(ByteString.copyFrom(value))
         .build();

      try {
         blockingStub.put(req);
      } catch (StatusRuntimeException e) {
         LOG.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      }
   }

   public byte[] get(byte[] key, String cacheName) {
      Key req = Key.newBuilder()
         .setCacheName(cacheName)
         .setKey(ByteString.copyFrom(key))
         .build();

      Value rsp;
      try {
         rsp = blockingStub.get(req);
         return rsp.getValue().toByteArray();
      } catch (StatusRuntimeException e) {
         LOG.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
         return null; // TODO Dealing with failures?
      }
   }

   public static void main(String[] args) throws Exception {
      InfinispanClient client = new InfinispanClient("localhost", 50051);
      final Charset ch = Charset.forName("UTF-8");
      try {
         client.put("hello".getBytes(ch), "world".getBytes(ch), "test");
         final byte[] value = client.get("hello".getBytes(ch), "test");
         LOG.info("Get after put returned: " + new String(value));
      } finally {
         client.shutdown();
      }
   }

}
