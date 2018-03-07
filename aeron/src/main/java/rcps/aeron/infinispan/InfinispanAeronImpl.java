package rcps.aeron.infinispan;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.util.Arrays;

public class InfinispanAeronImpl {

   private final EmbeddedCacheManager cacheContainer;

   public InfinispanAeronImpl() {
      cacheContainer = new DefaultCacheManager();
      cacheContainer.defineConfiguration(
         "test"
         , new ConfigurationBuilder().build()
      );
   }

   public void put(byte[] key, byte[] value, String cacheName) {
      System.out.printf(
         "[server, cache=%s] put(%s, %s)%n"
         , cacheName
         , Arrays.toString(key)
         , Arrays.toString(value)
      );

      final Cache<byte[], byte[]> cache = cacheContainer.getCache(cacheName);
      cache.put(key, value);
   }

   public byte[] get(byte[] key, String cacheName) {
      final Cache<byte[], byte[]> cache = cacheContainer.getCache(cacheName);
      final byte[] value = cache.get(key);

      System.out.printf(
         "[server, cache=%s] get(%s) = %s%n"
         , cacheName
         , Arrays.toString(key)
         , Arrays.toString(value)
      );

      return value;
   }

}
