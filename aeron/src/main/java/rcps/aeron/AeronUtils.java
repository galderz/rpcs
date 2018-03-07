package rcps.aeron;

import io.aeron.Publication;

public class AeronUtils {

   public static void offerResult(long result) {
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

}
