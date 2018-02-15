package rpcs.grpc.infinispan;

import io.grpc.stub.StreamObserver;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import rpcs.grpc.infinispan.InfinispanOuterClass.Empty;
import rpcs.grpc.infinispan.InfinispanOuterClass.Key;
import rpcs.grpc.infinispan.InfinispanOuterClass.KeyValue;
import rpcs.grpc.infinispan.InfinispanOuterClass.Value;

final class InfinispanImpl extends InfinispanGrpc.InfinispanImplBase {

   private final EmbeddedCacheManager cacheContainer;

   public InfinispanImpl() {
      cacheContainer = new DefaultCacheManager();
      cacheContainer.defineConfiguration("test",
         new ConfigurationBuilder().build());
   }

   @Override
   public void put(KeyValue req, StreamObserver<Empty> obs) {
      final String cacheName = req.getCacheName();

      // TODO Binary cache
      final Cache<String, String> cache = cacheContainer.getCache(cacheName);

      cache.put(req.getKey(), req.getValue());

      obs.onNext(Empty.newBuilder().build());
      obs.onCompleted();
   }

   @Override
   public void get(Key req, StreamObserver<Value> obs) {
      final String cacheName = req.getCacheName();

      // TODO Binary cache
      final Cache<String, String> cache = cacheContainer.getCache(cacheName);

      final String value = cache.get(req.getKey());

      obs.onNext(Value.newBuilder().setValue(value).build());
      obs.onCompleted();
   }

}
