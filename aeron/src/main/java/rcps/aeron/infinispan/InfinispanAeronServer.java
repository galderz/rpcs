package rcps.aeron.infinispan;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import rcps.aeron.infinispan.codec.EmptyEncoder;
import rcps.aeron.infinispan.codec.KeyDecoder;
import rcps.aeron.infinispan.codec.KeyValueDecoder;
import rcps.aeron.infinispan.codec.MessageHeaderDecoder;
import rcps.aeron.infinispan.codec.MessageHeaderEncoder;
import rcps.aeron.infinispan.codec.ValueEncoder;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static rcps.aeron.AeronUtils.offerResult;
import static rcps.aeron.infinispan.Constants.REP_CHANNEL;
import static rcps.aeron.infinispan.Constants.REQ_CHANNEL;
import static rcps.aeron.infinispan.Constants.STREAM_ID;

public class InfinispanAeronServer {

   static final MessageHeaderEncoder MSG_HEADER_ENCODER = new MessageHeaderEncoder();
   static final MessageHeaderDecoder MSG_HEADER_DECODER = new MessageHeaderDecoder();

   static final KeyValueDecoder KEY_VALUE_DECODER = new KeyValueDecoder();
   static final KeyDecoder KEY_DECODER = new KeyDecoder();

   static final EmptyEncoder EMPTY_ENCODER = new EmptyEncoder();
   static final ValueEncoder VALUE_ENCODER = new ValueEncoder();

   static final Aeron AERON = Aeron.connect(new Aeron.Context());
   static final Subscription SUBSCRIPTION = AERON.addSubscription(REQ_CHANNEL, STREAM_ID);
   static final Publication PUBLICATION = AERON.addPublication(REP_CHANNEL, STREAM_ID);

   static final FragmentHandler FRAGMENT_HANDLER = new FragmentAssembler(InfinispanAeronServer::onMessage);
   static final int FRAGMENT_LIMIT = 256;

   static final InfinispanAeronImpl infinispan = new InfinispanAeronImpl();

   public static void main(String[] args) throws InterruptedException {
      Thread receiver = new Thread(new Receiver());
      receiver.start();

      while(true) {
         Thread.sleep(1000);
      }
   }

   private static void onMessage(DirectBuffer buffer, int offset, int length, Header header) {
      final int msgOffset = MessageHeaderDecoder.ENCODED_LENGTH + offset;
      MSG_HEADER_DECODER.wrap(buffer, offset);
      switch (MSG_HEADER_DECODER.templateId()) {
         case KeyValueDecoder.TEMPLATE_ID:
            onKeyValue(buffer, msgOffset, MSG_HEADER_DECODER.blockLength(), MSG_HEADER_DECODER.version());
            break;
         case KeyDecoder.TEMPLATE_ID:
            onKey(buffer, msgOffset, MSG_HEADER_DECODER.blockLength(), MSG_HEADER_DECODER.version());
            break;
         default:
            throw new IllegalStateException("Unknown message template: " + MSG_HEADER_DECODER.templateId());
      }
   }

   private static void onKeyValue(DirectBuffer buffer, int offset, int length, int version) {
      KEY_VALUE_DECODER.wrap(buffer, offset, length, version);

      final String cacheName = KEY_VALUE_DECODER.cacheName();

      final int keyLength = KEY_VALUE_DECODER.keyLength();
      final byte[] keyBytes = new byte[keyLength];
      KEY_VALUE_DECODER.getKey(keyBytes, 0, keyLength);

      final int valueLength = KEY_VALUE_DECODER.valueLength();
      final byte[] valueBytes = new byte[valueLength];
      KEY_VALUE_DECODER.getValue(valueBytes, 0, valueLength);

      infinispan.put(keyBytes, valueBytes, cacheName);

      final ExpandableArrayBuffer buff = new ExpandableArrayBuffer(512);

      EMPTY_ENCODER
         .wrapAndApplyHeader(buff, 0, MSG_HEADER_ENCODER);

      long result = PUBLICATION.offer(buff, 0, MSG_HEADER_ENCODER.encodedLength() + EMPTY_ENCODER.encodedLength());
      offerResult(result);
   }

   private static void onKey(DirectBuffer buffer, int offset, int length, int version) {
      KEY_DECODER.wrap(buffer, offset, length, version);

      final String cacheName = KEY_DECODER.cacheName();

      final int keyLength = KEY_DECODER.keyLength();
      final byte[] keyBytes = new byte[keyLength];
      KEY_DECODER.getKey(keyBytes, 0, keyLength);

      final byte[] value = infinispan.get(keyBytes, cacheName);

      final ExpandableArrayBuffer buff = new ExpandableArrayBuffer(512);

      VALUE_ENCODER
         .wrapAndApplyHeader(buff, 0, MSG_HEADER_ENCODER)
         .putValue(value, 0, value.length);

      long result = PUBLICATION.offer(buff, 0, MSG_HEADER_ENCODER.encodedLength() + VALUE_ENCODER.encodedLength());
      offerResult(result);
   }

   private static class Receiver implements Runnable {

      @Override
      public void run() {
         while (true) {
            SUBSCRIPTION.poll(FRAGMENT_HANDLER, FRAGMENT_LIMIT);
         }
      }

   }

}
