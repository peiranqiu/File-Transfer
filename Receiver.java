import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * A receiver in the sliding window transport protocol that send out ACK segments to acknowledge
 * delivered segments. The protocol uses the go-back-n as recovery technique.
 */
public class Receiver {

  // Set average delay on receiver side.
  private static final double AVERAGE_DELAY = 100;

  public static void main(String[] args) {

    // Read command line args.
    checkArguments(args);
    int port = Integer.parseInt(args[0]);
    int win = Integer.valueOf(args[1]);
    String fileName = args[2];

    try {
      // Set up receiver socket.
      DatagramSocket socket = new DatagramSocket(port);
      System.out.println("Server started, listening on port " + port);

      File file = new File(fileName);
      OutputStream fileWriter = new FileOutputStream(file);

      // Expected segment sequence.
      int expected = 0;

      // Current receiving window.
      int current_window = 0;

      while (true) {

        // Receive Data segment.
        DatagramPacket request = new DatagramPacket(new byte[1024], 1024);
        socket.receive(request);

        // Deserialize the packet into segment.
        Segment segment = new Segment();
        segment.deserialize(request.getData());

        // Simulate delay upon packet arrival.
        Thread.sleep(Math.round(AVERAGE_DELAY * 2 * Math.random()));

        byte ack_type = 0x2;
        byte ack_win = new Integer(current_window).byteValue();
        short ack_length = (short) 0;
        byte[] ack_payload = new byte[0];

        InetAddress clientAddress = request.getAddress();
        int clientPort = request.getPort();

        System.out.println("DATA received: " + segment.getSequence());
        if (segment.getSequence() == expected) {

          // Write data into output file if it's expected.
          fileWriter.write(segment.getPayload());
          System.out.println("Segment " + segment.getSequence() + " has been received and written into file.");

          // Create acknowledgement segment.
          short ack_sequence = new Integer(expected + 1).shortValue();
          Segment ack_segment = new Segment(ack_type, ack_win, ack_sequence, ack_length, ack_payload);

          // Send acknowledgement segment back.
          byte[] byteArray = ack_segment.serialize();
          DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, clientAddress, clientPort);
          socket.send(packet);

          // Check if it's the end of data transit.
          if (segment.getLength() < 512) {
            System.out.println("All acknowledgement segments have been sent out.");
            fileWriter.close();
            System.exit(0);
          } else {
            // Move to next window.
            expected++;
            current_window = (current_window + 1) % win;
          }

        } else {
          System.out.println("Segment " + expected + " is expected. Segment " + segment.getSequence() + " is discarded");

          // Create acknowledgement segment.
          short ack_sequence = new Integer(expected).shortValue();
          Segment ack_segment = new Segment(ack_type, ack_win, ack_sequence, ack_length, ack_payload);

          // Send acknowledgement segment back.
          byte[] byteArray = ack_segment.serialize();
          DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, clientAddress, clientPort);
          socket.send(packet);
        }
      }
    } catch (IOException e) {
      System.out.println("I/O Error: " + e.getMessage());
    } catch (InterruptedException i) {
      System.out.println("Unexpected Interruption: " + i.getMessage());
    }

  }

  /**
   * Handle command line argument errors. Should be <listening_port_number> <window_size>
   * <output_file>
   *
   * @param args the passed args
   */
  private static void checkArguments(String[] args) {

    if (args.length != 3) {
      System.out.println("Expected 3 arguments: <listening_port_number> <window_size> <output_file>");
      System.exit(0);
    }
    if (Integer.valueOf(args[1]) < 1 || Integer.valueOf(args[1]) > 7) {
      System.out.println("Expected window size should be larger than 0 and smaller than 8.");
      System.exit(0);
    }
  }
}
