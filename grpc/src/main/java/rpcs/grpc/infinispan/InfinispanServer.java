package rpcs.grpc.infinispan;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

public class InfinispanServer {

   private static final Logger LOG =
      Logger.getLogger(InfinispanServer.class.getName());

   private Server server;

   private void start() throws IOException {
      /* The port on which the server should run */
      int port = 50051;
      server = ServerBuilder.forPort(port)
         .addService(new InfinispanImpl())
         .build()
         .start();
      LOG.info("Server started, listening on " + port);
      Runtime.getRuntime().addShutdownHook(new Thread() {
         @Override
         public void run() {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            InfinispanServer.this.stop();
            System.err.println("*** server shut down");
         }
      });
   }

   private void stop() {
      if (server != null) {
         server.shutdown();
      }
   }

   /**
    * Await termination on the main thread since the grpc library uses daemon threads.
    */
   private void blockUntilShutdown() throws InterruptedException {
      if (server != null) {
         server.awaitTermination();
      }
   }

   public static void main(String[] args) throws IOException, InterruptedException {
      final InfinispanServer server = new InfinispanServer();
      server.start();
      server.blockUntilShutdown();
   }

}
