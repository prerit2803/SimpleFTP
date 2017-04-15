import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

class Segment {
	int index;
	Segment next;
	byte[] data;
	public Segment(int index, byte[] data) {
		this.index = index;
		this.next = null;
		this.data = data;
	}
}

public class Client {
	private static Segment head;

	public Client() {
		head = null;
	}

	public static void main(String[] args) throws IOException {
		// inputs
		Scanner scan = new Scanner(System.in);
		String hostname = scan.next();
		int port = scan.nextInt();
		String filename = scan.next();
		int N = scan.nextInt();
		int mss = scan.nextInt();
		scan.close();

		// Socket
		DatagramSocket clientSocket = null;
		try {
			clientSocket = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		InetAddress serverIP = null;
		try {
			serverIP = InetAddress.getByName(hostname);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		Path filePath = Paths.get(filename);
		byte[] dataPacket = null;
		try {
			dataPacket = Files.readAllBytes(filePath);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		chunksDivision(dataPacket, mss);
		int currentIndex = 0;
		int pointer = 0;
		int seqNumber = 0;
		while (currentIndex * mss < dataPacket.length) {
			while (pointer < N) {// sending
				Segment temp = head;
				while (temp.index != currentIndex) // searching for data to send
					temp = temp.next;
				byte[] data = temp.data;
				byte[] header = createHeader(seqNumber, data); // creating
																// header
				byte[] packetToSend = new byte[header.length + mss];
				for (int i = 0; i < packetToSend.length; i++) { // copying
																// header + data
					if (i < header.length)
						packetToSend[i] = header[i];
					else
						packetToSend[i] = data[i];
				}
				DatagramPacket toReceiver = new DatagramPacket(packetToSend, packetToSend.length, serverIP, port);
				try {// sending packet to server
					clientSocket.send(toReceiver);
					currentIndex++;
					pointer++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// in receiving mode
			int timeout = 1000;
			byte[] receive = new byte[1024];
			DatagramPacket fromReceiver = new DatagramPacket(receive, receive.length);
			boolean flag = true;
			int seqAck = 0;
			try {
				clientSocket.setSoTimeout(timeout);
				while (flag) {
					clientSocket.receive(fromReceiver);
					seqAck = ackHandler(fromReceiver.getData());
					if (seqAck == currentIndex - 1) { // latest packet
														// acknowledgement
						pointer = 0;
						flag = false;
					} else if (seqAck != -1) { // any other acknowledgement
						pointer = currentIndex - seqAck - 1;
						currentIndex = seqAck + 1;
					}
				}
			} catch (SocketTimeoutException ste) {// timeout
				System.out.println("Timeout, sequence number = " + seqAck);
			}
		}
	}

	public static void chunksDivision(byte[] dataPacket, int MSS) {
		int totalPackets = dataPacket.length / MSS;
		for (int i = 0; i < totalPackets; i++) {
			byte[] segment = new byte[MSS];
			segment = Arrays.copyOfRange(dataPacket, MSS * i, MSS * (i + 1));
			Segment s = new Segment(i, segment);
			if (head == null) {
				head = s;
			} else {
				Segment temp = head;
				while (temp.next != null) {
					temp = temp.next;
				}
				temp.next = s;
			}

		}
	}

	public static byte[] createHeader(int sequence, byte[] data) {
		String sequenceStr = Integer.toBinaryString(sequence);
		String checksum = checksumCalculation(data);
		String fixedVal = "0101010101010101";
		String header = sequenceStr + checksum + fixedVal;
		return header.getBytes();
	}

	public static String checksumCalculation(byte[] data) {
		int dataLength = data.length;
		int Checksum = 0;
		int i = 0;
		while (dataLength > 0) {
			int Word = ((data[i] << 8) + data[i + 1]) + Checksum;

			Checksum = Word & 0x0FFFF;

			Word = (Word >> 16);

			Checksum = Word + Checksum;

			dataLength -= 2;
			i += 2;
		}
		Checksum = ~Checksum;
		return Integer.toString(Checksum);
	}

	public static int ackHandler(byte[] data) {
		String ACK = Arrays.toString(data);
		String packetType = ACK.substring(48, 64);
		if (packetType.equals("1010101010101010")) {
			return binToDec(ACK.substring(0, 32));
		}
		return -1;
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
