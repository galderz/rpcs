package rcps.aeron.infinispan;

import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import io.aeron.samples.SampleConfiguration;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import rcps.aeron.infinispan.codec.EmptyDecoder;
import rcps.aeron.infinispan.codec.KeyEncoder;
import rcps.aeron.infinispan.codec.KeyValueDecoder;
import rcps.aeron.infinispan.codec.KeyValueEncoder;
import rcps.aeron.infinispan.codec.MessageHeaderDecoder;
import rcps.aeron.infinispan.codec.MessageHeaderEncoder;
import rcps.aeron.infinispan.codec.ValueDecoder;

import java.nio.charset.Charset;

public class InfinispanAeronClient {

   private static final int STREAM_ID = 20;
   private static final String CHANNEL = "aeron:udp?endpoint=localhost:40133";

   private static final int FRAGMENT_LIMIT = 256;
   
//   static final int DEFAULT_RETRY_ATTEMPTS = 3;
//
//   static final IdleStrategy RETRY_IDLE_STRATEGY = new BusySpinIdleStrategy();

   static final MessageHeaderEncoder MSG_HEADER_ENCODER = new MessageHeaderEncoder();
   static final MessageHeaderDecoder MSG_HEADER_DECODER = new MessageHeaderDecoder();

   static final KeyValueEncoder KEY_VALUE_ENCODER = new KeyValueEncoder();
   static final KeyEncoder KEY_ENCODER = new KeyEncoder();

   static final EmptyDecoder EMPTY_DECODER = new EmptyDecoder();
   static final ValueDecoder VALUE_DECODER = new ValueDecoder();

   static final FragmentHandler fragmentHandler = new FragmentAssembler(InfinispanAeronClient::onMessage);

   static final Aeron AERON = Aeron.connect(new Aeron.Context());
   static final Publication PUBLICATION = AERON.addPublication(CHANNEL, STREAM_ID);
   static final Subscription SUBSCRIPTION = AERON.addSubscription(CHANNEL, STREAM_ID);

   public static void main(String[] args) {
      final Thread receiverThread = new Thread(new Receiver());
      receiverThread.start();

      final ExpandableArrayBuffer outboundBuffer = new ExpandableArrayBuffer(512);

      final Charset ch = Charset.forName("UTF-8");
      final byte[] key = "hello".getBytes(ch);
      final byte[] value = "world".getBytes(ch);

      KEY_VALUE_ENCODER
         .wrapAndApplyHeader(outboundBuffer, 0, MSG_HEADER_ENCODER)
         .cacheName("test")
         .putKey(key, 0, key.length)
         .putValue(value, 0, value.length);

      long result = PUBLICATION.offer(outboundBuffer, 0, KEY_VALUE_ENCODER.encodedLength());
      handleResult(result);

      if (!PUBLICATION.isConnected()) {
         System.out.println("No active subscribers detected");
      }

      KEY_ENCODER
         .wrapAndApplyHeader(outboundBuffer, 0, MSG_HEADER_ENCODER)
         .cacheName("test")
         .putKey(key, 0, key.length);

      result = PUBLICATION.offer(outboundBuffer, (int) result, KEY_VALUE_ENCODER.encodedLength());
      handleResult(result);
   }

   private static void handleResult(long result) {
      if (result < 0L) {
         if (result == Publication.BACK_PRESSURED) {
            System.out.println("Offer failed due to back pressure");
         } else if (result == Publication.NOT_CONNECTED) {
            System.out.println("Offer failed because publisher is not connected to subscriber");
         } else if (result == Publication.ADMIN_ACTION) {
            System.out.println("Offer failed because of an administration action in the system");
         } else if (result == Publication.CLOSED) {
            System.out.println("Offer failed publication is closed");
         } else if (result == Publication.MAX_POSITION_EXCEEDED) {
            System.out.println("Offer failed due to publication reaching max position");
         } else {
            System.out.println("Offer failed due to unknown reason");
         }
      } else {
         System.out.println("yay!");
      }
   }

   private static void onMessage(DirectBuffer buffer, int offset, int length, Header header) {
      final int msgOffset = MessageHeaderDecoder.ENCODED_LENGTH + offset;
      MSG_HEADER_DECODER.wrap(buffer, offset);
      switch (MSG_HEADER_DECODER.templateId()) {
         case EmptyDecoder.TEMPLATE_ID:
            onEmpty(buffer, msgOffset, MSG_HEADER_DECODER.blockLength(), MSG_HEADER_DECODER.version());
            break;
         case ValueDecoder.TEMPLATE_ID:
            onValue(buffer, msgOffset, MSG_HEADER_DECODER.blockLength(), MSG_HEADER_DECODER.version());
            break;
         default:
            throw new IllegalStateException("Unknown message template");
      }
   }

   private static void onEmpty(DirectBuffer buffer, int offset, int length, int version) {
      EMPTY_DECODER.wrap(buffer, offset, length, version);
      System.out.println("Put responded");
   }

   private static void onValue(DirectBuffer buffer, int offset, int length, int version) {
      VALUE_DECODER.wrap(buffer, offset, length, version);
      final int valueLength = VALUE_DECODER.valueLength();
      final byte[] bytes = new byte[valueLength];
      VALUE_DECODER.getValue(bytes, 0, valueLength);
      System.out.printf("Get responded: %s%n", new String(bytes));
   }

   private static class Receiver implements Runnable {

      @Override
      public void run() {
         while (true) {
            SUBSCRIPTION.poll(fragmentHandler, FRAGMENT_LIMIT);
         }
      }

   }

}
