import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * A sender in the sliding window transport protocol that send out more than one segment without
 * being forced to wait for an acknowledgement. The protocol uses the go-back-n as recovery
 * technique.
 */
public class Sender {

  private static final int PAYLOAD_SIZE = 512;
  // Set packet loss probability and retransmission timer.
  private static final int TIMER = 1000;
  private static final double PROBABILITY = 0.1;

  public static void main(String[] args) {

    // Read command line args.
    checkArguments(args);
    String dns = args[0];
    int port = Integer.parseInt(args[1]);
    byte win = Byte.valueOf(args[2]);
    File file = new File(args[3]);

    try {

      // Create sender socket.
      InetAddress address = InetAddress.getByName(dns);
      DatagramSocket socket = new DatagramSocket();

      // Read input file.
      InputStream inputStream = new FileInputStream(file);

      int readBytes = 0;
      byte[] buffer = new byte[PAYLOAD_SIZE];
      short sequence = 0;
      byte type = 0x1;
      ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();

      // Create segments.
      while ((readBytes = inputStream.read(buffer)) != -1) {
        dataBuffer.write(buffer, 0, readBytes);
        sequence++;
      }
      byte[] allBytes = dataBuffer.toByteArray();
      Segment[] segments = new Segment[sequence];
      for (int i = 0; i < sequence; i++) {
        byte[] payload = Arrays.copyOfRange(allBytes, i * PAYLOAD_SIZE, Math.min((i + 1) * PAYLOAD_SIZE, allBytes.length - 1));
        Segment s = new Segment(type, win, (short) i, (short) payload.length, payload);
        segments[i] = s;
      }

      System.out.println("Input file of size " + file.length() + " bytes has been split into " + sequence + " segments.");
      inputStream.close();

      // Last sent segment sequence.
      int sent = 0;
      // Last acknowledged segment sequence.
      int ack = -1;

      while (true) {

        // Send the packet.
        while (sent - ack < win && sent < sequence) {

          // Simulate packet loss upon sending.
          if (Math.random() > PROBABILITY) {
            Segment segment = segments[sent];
            byte[] byteArray = segment.serialize();
            DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, address, port);
            socket.send(packet);
            System.out.println("Segment " + sent + " has been sent out.");
          } else {
            System.out.println("Segment " + sent + " has been sent out but lost.");
          }
          sent++;
        }

        try {

          // Receive packet.
          socket.setSoTimeout(TIMER);
          DatagramPacket response = new DatagramPacket(new byte[1024], 1024);
          socket.receive(response);

          // Deserialize the packet into segment.
          Segment segment = new Segment();
          segment.deserialize(response.getData());

          System.out.println("ACK recieved: " + segment.getSequence());
          // Update last acknowledged sequence.
          if (segment.getSequence() - 1 == ack + 1) {
            ack++;
            System.out.println("Segment " + ack + " has been acknowledged.");
          }

          // Stop receiving packets when all segments have been acknowledged.
          if (segment.getSequence() == sequence) {
            System.out.println("All data segments have been acknowledged.");
            System.exit(0);
          }

        } catch (SocketTimeoutException e) {

          // Retransmit unacknowledged packets.
          for (int i = ack + 1; i < sent; i++) {
            // Simulate packet loss upon sending.
            if (Math.random() > PROBABILITY) {
              byte[] byteArray = segments[i].serialize();
              DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, address, port);
              socket.send(packet);
              System.out.println("Segment " + i + " has been resent.");
            } else {
              System.out.println("Segment " + i + " has been resent but lost.");
            }
          }

        }

      }


    } catch (FileNotFoundException f) {
      System.out.println("Error: File not found.");
    } catch (UnknownHostException e) {
      System.out.println("Error: Unknown host.");
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }

  }


  /**
   * Handle command line argument errors. Should be <destination_DNS_name> <destination_port_number>
   * <window_size> <input_file>
   *
   * @param args the passed args
   */
  private static void checkArguments(String[] args) {

    if (args.length != 4) {
      System.out.println("Expected 4 arguments: <destination_DNS_name> <destination_port_number> <window_size> <input_file>");
      System.exit(0);
    }
    if (Integer.valueOf(args[2]) < 1 || Integer.valueOf(args[2]) > 7) {
      System.out.println("Expected window size should be larger than 0 and smaller than 8.");
      System.exit(0);
    }
    File file = new File(args[3]);
    if (file.length() > 256 * 512 - 1 || file.length() < 1) {
      System.out.println("Please use a binary file size larger than 0 and smaller than 128 kb.");
      System.exit(0);
    }
  }
}
