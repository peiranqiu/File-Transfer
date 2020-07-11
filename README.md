## Sliding Window Protocol

In this sliding window protocol, a go-back-n method is used for retransmission. And following assumptions are made. They can be changed either in arguments or at the beginning of java source files.

1. AVERAGE_DELAY = 100;
   TIMER = 1000;
   LOSS_PROBABILITY = 0.1;

2. Server address: 10.10.10.100

   Port number: 8000

   window size: 4

   

Follow the below instructions to run the app.

1. Go to folder containing the java files

   ```
   cd tp
   ```

2. Compile the three java files

   ```bash
   javac Segment.java Receiver.java Sender.java 
   ```

3. Run receiver

   ```bash
   java Receiver 8000 4 output
   ```

4. Run sender in another terminal window under same directory

   ```bash
   java Sender 10.10.10.100 8000 4 input
   ```

5. The transportation process get logged. Sender and receiver will exit after transportation finished.

```bash
Server started, listening on port 8000
DATA received: 0
Segment 0 has been received and written into file.
DATA received: 6
Segment 5 is expected. Segment 6 is discarded
All acknowledgement segments have been sent out.
```



