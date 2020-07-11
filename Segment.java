import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A segment contains data or acknowledgement that are transmitted over sender and receiver.
 */
public class Segment implements Serializable {

  private byte type;
  private byte win;
  private short sequence;
  private short length;
  private byte[] payload;

  /**
   * Initialize an empty segment.
   */
  public Segment() {

    this.type = 0x2;
    this.win = 0;
    this.sequence = 0;
    this.length = 0;
    this.payload = new byte[512];

  }

  /**
   * Construct a segment.
   *
   * @param type     type of the segment
   * @param win      size of current window
   * @param sequence sequence number
   * @param length   length of the payload
   * @param payload  data to send
   */
  public Segment(byte type, byte win, short sequence, short length, byte[] payload) {

    this.type = type;
    this.win = win;
    this.sequence = sequence;
    this.length = length;
    this.payload = payload;

  }

  public void setType(byte type) {
    this.type = type;
  }

  public void setWin(byte win) {
    this.win = win;
  }

  public void setSequence(short sequence) {
    this.sequence = sequence;
  }

  public void setLength(short length) {
    this.length = length;
  }

  public void setPayload(byte[] payload) {
    this.payload = payload;
  }

  public byte getType() {
    return type;
  }

  public byte getWin() {
    return win;
  }

  public short getSequence() {
    return sequence;
  }

  public short getLength() {
    return length;
  }

  public byte[] getPayload() {
    return payload;
  }

  /**
   * Convert the segment object to a byte array to send over.
   *
   * @return the converted byte array
   */
  public byte[] serialize() {

    byte[] bytes = new byte[1024];

    try {
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
      objectStream.writeObject(this);
      bytes = byteStream.toByteArray();

      byteStream.close();
      objectStream.close();

    } catch (IOException e) {
      System.out.println("I/O Error: " + e.getMessage());
    } finally {
      return bytes;
    }

  }

  /**
   * Deserialize data from a given byte array to the segment.
   */
  public void deserialize(byte[] bytes) {

    try {
      ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
      ObjectInputStream objectStream = new ObjectInputStream(byteStream);
      Segment segment = (Segment) objectStream.readObject();

      this.setLength(segment.getLength());
      this.setType(segment.getType());
      this.setWin(segment.getWin());
      this.setSequence(segment.getSequence());
      this.setPayload(segment.getPayload());

      byteStream.close();
      objectStream.close();

    } catch (EOFException o) {
      System.out.println("Could not read object.");

    } catch (IOException e) {
      System.out.println("I/O Error: " + e.getMessage());

    } catch (ClassNotFoundException c) {
      System.out.println("Class not found: " + c.getMessage());
    }

  }

}
