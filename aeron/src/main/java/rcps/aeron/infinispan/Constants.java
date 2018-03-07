package rcps.aeron.infinispan;

final class Constants {

   static final int STREAM_ID = 10;
   static final String REQ_CHANNEL = "aeron:udp?endpoint=localhost:40133";
   static final String REP_CHANNEL = "aeron:udp?endpoint=localhost:40134";

   private Constants() {
   }

}
