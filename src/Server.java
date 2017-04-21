import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

class ReceivedSegment {
	int index;
	ReceivedSegment next;
	String data;

	public ReceivedSegment(int index, String data) {
		this.index = index;
		this.next = null;
		this.data = data;
	}
}

public class Server {
	private static ReceivedSegment head;

	public Server() {
		head = null;
	}

	public static void main(String[] args) {
		// input
		System.out.println("This is server");
		Scanner scan = new Scanner(System.in);
		int port = scan.nextInt();
		String filename = scan.next();
		double pos = scan.nextDouble();
		scan.close();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int currentIndex = 0;
		DatagramSocket serverSocket = null;
		try {
			serverSocket = new DatagramSocket(port);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		DatagramPacket fromClient = null;
		boolean flag = true;
		while (flag) {
			try {
				// System.out.println("Receiving");
				byte[] dataPacket = new byte[2048];
				fromClient = new DatagramPacket(dataPacket, dataPacket.length);
				serverSocket.receive(fromClient);
				double rand = Math.random();
				// System.out.println("random number : " + rand);
				String data = new String(fromClient.getData()).substring(0, fromClient.getLength());
				// System.out.println("Data : " + data);
				int seqNumber = binToDec(data.substring(0, 32));
				System.out.println("Packet recieved : " + seqNumber);
				int checksum = binToDec(data.substring(32, 48));
				// System.out.println("checksum: " + checksum);
				String packetType = data.substring(48, 64);
				// System.out.println("pckt type : " + packetType);
				String dataIn = data.substring(64, data.length());
				// System.out.println(" data: " + dataIn);
				if (packetType.equals("0000000000000000")) {// when EOF is send
					flag = false;
					break;
				}
				if (rand <= pos) {
					System.out.println("Packet loss, sequence number = " + seqNumber);
					continue;
				} else if (receive(dataIn, checksum) == 0 && seqNumber == currentIndex) {
					// System.out.println("checksum is okay");
					ReceivedSegment segmentClient = new ReceivedSegment(currentIndex, dataIn);
					if (head == null)
						head = segmentClient;
					else {
						ReceivedSegment temp = head;
						while (temp.next != null)
							temp = temp.next;
						temp.next = segmentClient;
					}
					// System.out.println("writing: "+dataIn);
					baos.write(dataIn.getBytes());
					// System.out.println("insertion done");
					InetAddress IP = fromClient.getAddress();
					int portNumber = fromClient.getPort();
					byte[] acknowledgement = ackSender(seqNumber);
					DatagramPacket toClient = new DatagramPacket(acknowledgement, acknowledgement.length, IP,
							portNumber);
					serverSocket.send(toClient);
					System.out.println("ACK sent for: " + seqNumber);
					currentIndex++;
				}
			} catch (Exception e) {
				System.err.println(e);
			}
		}
		// System.out.println("Writing to file");
		FileOutputStream file = null;
		try {
			file = new FileOutputStream(filename);
			// ReceivedSegment temp = head;
			// while (temp != null) {
			// String writeData = temp.data;
			// System.out.print(writeData+" ");
			// baos.write(writeData.getBytes());
			// }
			baos.writeTo(file);
			file.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		serverSocket.close();
	}

	public static byte[] ackSender(int seqNo) {
		String header = Integer.toBinaryString(seqNo);
		for (int i = header.length(); i < 32; i++) {
			header = "0" + header;
		}
		header = header + "00000000000000001010101010101010";
		// System.out.println("header is: " + header);
		return header.getBytes();
	}

	public static int generateChecksum(String s) {
//		 System.out.println("checksum for " + s);
		String hex_value = new String();
		// 'hex_value' will be used to store various hex values as a string
		int x, i, checksum = 0;
		// 'x' will be used for general purpose storage of integer values
		// 'i' is used for loops
		// 'checksum' will store the final checksum
		for (i = 0; i < s.length() - 2; i = i + 2) {
			x = (int) (s.charAt(i));
			hex_value = Integer.toHexString(x);
			x = (int) (s.charAt(i + 1));
			hex_value = hex_value + Integer.toHexString(x);
			// Extract two characters and get their hexadecimal ASCII values
//			 System.out.println(s.charAt(i) + "" + s.charAt(i + 1) + " : " +
//			 hex_value);
			x = Integer.parseInt(hex_value, 16);
//			System.out.println("x: "+x);
			// Convert the hex_value into int and store it
			checksum += x;
			// Add 'x' into 'checksum'
		}
		if (s.length() % 2 == 0) {
			// If number of characters is even, then repeat above loop's steps
			// one more time.
			x = (int) (s.charAt(i));
			hex_value = Integer.toHexString(x);
			x = (int) (s.charAt(i + 1));
			hex_value = hex_value + Integer.toHexString(x);
//			 System.out.println(s.charAt(i) + "" + s.charAt(i + 1) + " : " +
//			 hex_value);
			x = Integer.parseInt(hex_value, 16);
		} else {
			// If number of characters is odd, last 2 digits will be 00.
			x = (int) (s.charAt(i));
			hex_value = "00" + Integer.toHexString(x);
			x = Integer.parseInt(hex_value, 16);
//			 System.out.println(s.charAt(i) + " : " + hex_value);
		}
		checksum += x;
		// Add the generated value of 'x' from the if-else case into 'checksum'
		hex_value = Integer.toHexString(checksum);
//		System.out.println("hex: "+hex_value+" len: "+ hex_value.length());
		// Convert into hexadecimal string
		if (hex_value.length() > 4) {
			// If a carry is generated, then we wrap the carry
			int carry = Integer.parseInt(("" + hex_value.charAt(0)), 16);
			// Get the value of the carry bit
			hex_value = hex_value.substring(1, 5);
			// Remove it from the string
			checksum = Integer.parseInt(hex_value, 16);
			// Convert it into an int
			checksum += carry;
			// Add it to the checksum
		}
		checksum = generateComplement(checksum);
//		System.out.println("final checksum: "+checksum);
		// Get the complement
		return checksum;
	}

	public static int generateComplement(int checksum) {
		// System.out.println("received checksum in comp: " + checksum);
		// Generates 15's complement of a hexadecimal value
		checksum = Integer.parseInt("FFFF", 16) - checksum;
		// System.out.println("comp checksum in comp: " + checksum);
		return checksum;
	}

	public static int receive(String s, int checksum) {
		int generated_checksum = generateChecksum(s);
		// System.out.println("received checksum: " +
		// Integer.toBinaryString(checksum));
		// System.out.println("generated checksum: " +
		// Integer.toBinaryString(generated_checksum));
		// Calculate checksum of received data
		generated_checksum = generateComplement(generated_checksum);
		// System.out.println("comp checksum: " +
		// Integer.toBinaryString(generated_checksum));
		// System.out.println("checksum comp: " +
		// Integer.toBinaryString(generated_checksum));
		// Then get its complement, since generated checksum is complemented
		int syndrome = generated_checksum + checksum;
		// Syndrome is addition of the 2 checksums
		syndrome = generateComplement(syndrome);
		// It is complemented
		 System.out.println("Syndrome is= " + syndrome);
		return syndrome;
	}

	private static int binToDec(String substring) {
		int dec = 0;
		int power = 0;
		for (int i = substring.length() - 1; i >= 0; i--) {
			if (substring.charAt(i) == '1')
				dec += Math.pow(2, power);
			power++;
		}
		return dec;
	}
}
