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
import java.util.Scanner;

class Segment {
	int index;
	Segment next;
	String data;

	public Segment(int index, String data) {
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
		System.out.println("This is client");
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
		int seqAck = -1;
		
		while ((currentIndex * mss) < dataPacket.length) { 
			while (pointer < N && (currentIndex * mss) < dataPacket.length) {// sending
				Segment temp = head;
				while (temp.index != currentIndex) // searching for data to send
					temp = temp.next;
				String data = temp.data;
				byte[] header = createHeader(currentIndex, data); // creating
																// header
				byte[] dataBytes = data.getBytes();
				byte[] packetToSend = new byte[header.length + dataBytes.length];
				for (int i = 0, j=0; i < packetToSend.length ; i++) { // copying
																// header + data
					if (i < header.length)
						packetToSend[i] = header[i];
					else{
						packetToSend[i] = dataBytes[j];
						j++;
				}}
				DatagramPacket toReceiver = new DatagramPacket(packetToSend, packetToSend.length, serverIP, port);
				try {// sending packet to server
					clientSocket.send(toReceiver);
					System.out.println("Packet sent : " +currentIndex);
					currentIndex++;
					pointer++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// in receiving mode
//			System.out.println("receiving with :"+pointer +"currentIndex"+currentIndex);
			int timeout = 1000;// in milliseconds
			byte[] receive = new byte[1024];
			DatagramPacket fromReceiver = new DatagramPacket(receive, receive.length);
			boolean flag = true;
			
			int tempIndex = currentIndex;
			try {
				clientSocket.setSoTimeout(timeout);
				while (flag) {
					clientSocket.receive(fromReceiver);
					seqAck = ackHandler(fromReceiver.getData());
					System.out.println("Ack received for : "+seqAck);
					if (seqAck == tempIndex - 1) { // latest packet
														// acknowledgement
						pointer = 0;
						currentIndex=tempIndex;
						flag = false;
					} else if (seqAck != -1) { // any other acknowledgement
						pointer = currentIndex - seqAck - 1;
						currentIndex = seqAck + 1;
					}
				}
			} catch (SocketTimeoutException ste) {// timeout
				System.out.println("Timeout, sequence number = " + seqAck);
				currentIndex=seqAck+1;
				pointer=0;
			}
		}
		// EOF
		String eof = "000000000000000000000000000000000000000000000000000000000000000000000000000";
		byte[] sendeof = eof.getBytes();
		DatagramPacket eofPacket = new DatagramPacket(sendeof, sendeof.length, serverIP, port);
		clientSocket.send(eofPacket);
	}

	public static void chunksDivision(byte[] dataPacket, int MSS) {
		int totalPackets = (int) Math.ceil((double) dataPacket.length / MSS);
		String dataString = new String(dataPacket); // nhi chle toh utf add kre
													// :D
		for (int i = 0; i < totalPackets; i++) {
			int j = MSS * (i + 1);
			if (j > dataString.length()) {
				j = dataString.length();
			}
			String seg = dataString.substring(MSS * i, j);
			Segment s = new Segment(i, seg);
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

	public static byte[] createHeader(int sequence, String data) {
		String sequenceStr = Integer.toBinaryString(sequence);
		String checksum = generateChecksum(data);
		String fixedVal = "0101010101010101";
		for (int i = sequenceStr.length(); i < 32; i++) {
			sequenceStr = "0" + sequenceStr;
		}
		String header = sequenceStr + checksum + fixedVal;
//		System.out.println("header is"+header);
		return header.getBytes();
	}

	public static String generateChecksum(String s) {
//		System.out.println("checksum for " + s);
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
//			System.out.println(s.charAt(i) + "" + s.charAt(i + 1) + " : " + hex_value);
			x = Integer.parseInt(hex_value, 16);
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
//			System.out.println(s.charAt(i) + "" + s.charAt(i + 1) + " : " + hex_value);
			x = Integer.parseInt(hex_value, 16);
		} else {
			// If number of characters is odd, last 2 digits will be 00.
			x = (int) (s.charAt(i));
			hex_value = "00" + Integer.toHexString(x);
			x = Integer.parseInt(hex_value, 16);
//			System.out.println(s.charAt(i) + " : " + hex_value);
		}
		checksum += x;
		// Add the generated value of 'x' from the if-else case into 'checksum'
		hex_value = Integer.toHexString(checksum);
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
		// Get the complement
		String padding =Integer.toBinaryString(checksum);
		for(int h=padding.length(); h<16; h++){
			padding = "0"+ padding;
		}
		return padding;
	}

	public static int generateComplement(int checksum) {
		// Generates 15's complement of a hexadecimal value
		checksum = Integer.parseInt("FFFF", 16) - checksum;
		return checksum;
	}

	public static int ackHandler(byte[] data) {
		String ACK = "";// Arrays.toString(data);
		for(int i=0; i<64;i++){
			if(data[i]==48){
				ACK += "0";
			}
			else{
				ACK +="1";
			}
		}
//		System.out.println("ACK data: "+ACK);
		String packetType = ACK.substring(48, 64);
//		System.out.println("ACK type: "+packetType);
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
