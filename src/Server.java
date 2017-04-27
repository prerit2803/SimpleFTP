import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Scanner;

public class Server {

	public static void main(String[] args) {
		// input
		System.out.println("This is Go Back N ARQ server");
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
			e1.printStackTrace();
		}

		DatagramPacket fromClient = null;
		boolean flag = true;
		while (flag) {
			try {
				byte[] dataPacket = new byte[2048];
				fromClient = new DatagramPacket(dataPacket, dataPacket.length);
				serverSocket.receive(fromClient);
				double rand = Math.random();
				String data = new String(fromClient.getData()).substring(0, fromClient.getLength());
				int seqNumber = binToDec(data.substring(0, 32));
				System.out.println("Packet received : " + seqNumber);
				int checksum = binToDec(data.substring(32, 48));
				String packetType = data.substring(48, 64);
				String dataIn = data.substring(64, data.length());
				if (packetType.equals("0000000000000000")) {// when EOF is send
					flag = false;
					break;
				}
				if (rand <= pos) {
					System.out.println("Packet loss, sequence number = " + seqNumber);
					continue;
				} else if (checksumValidation(dataIn, checksum) == 0 && seqNumber == currentIndex) {
					baos.write(dataIn.getBytes());
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
		FileOutputStream file = null;
		try {
			file = new FileOutputStream(filename);
			baos.writeTo(file);
			file.close();
		} catch (FileNotFoundException e) {
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
		return header.getBytes();
	}

	public static int checksumCalculation(String data) {
		String hexString = new String();
		int value, i, result = 0;
		for (i = 0; i < data.length() - 2; i = i + 2) {
			value = (int) (data.charAt(i));
			hexString = Integer.toHexString(value);
			value = (int) (data.charAt(i + 1));
			hexString = hexString + Integer.toHexString(value);
			value = Integer.parseInt(hexString, 16);
			result += value;
		}
		if (data.length() % 2 == 0) {
			value = (int) (data.charAt(i));
			hexString = Integer.toHexString(value);
			value = (int) (data.charAt(i + 1));
			hexString = hexString + Integer.toHexString(value);
			value = Integer.parseInt(hexString, 16);
		} else {
			value = (int) (data.charAt(i));
			hexString = "00" + Integer.toHexString(value);
			value = Integer.parseInt(hexString, 16);
		}
		result += value;
		hexString = Integer.toHexString(result);
		if (hexString.length() > 4) {
			int carry = Integer.parseInt(("" + hexString.charAt(0)), 16);
			hexString = hexString.substring(1, 5);
			result = Integer.parseInt(hexString, 16);
			result += carry;
		}
		result = Integer.parseInt("FFFF", 16) - result;
		return result;
	}

	public static int checksumValidation(String data, int oldChecksum) {
		int newChecksum = checksumCalculation(data);
		newChecksum = Integer.parseInt("FFFF", 16) - newChecksum;
		int valid = newChecksum + oldChecksum;
		valid = Integer.parseInt("FFFF", 16) - valid;
		return valid;
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
