package selectiveRepeat;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

public class SelectiveServer {
	private static ReceivedSegment head;

	public SelectiveServer() {
		head = null;
	}

	public static void main(String[] args) {
		// input
		System.out.println("This is Selective Repeat server");
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
				} else if (receive(dataIn, checksum) == 0) {
					InetAddress IP = fromClient.getAddress();
					int portNumber = fromClient.getPort();
					byte[] acknowledgement = ackSender(seqNumber);
					DatagramPacket toClient = new DatagramPacket(acknowledgement, acknowledgement.length, IP,
							portNumber);
					serverSocket.send(toClient);
					System.out.println("ACK sent for: " + seqNumber);
					if (seqNumber == currentIndex) {
						baos.write(dataIn.getBytes());
						System.out.println("writing: "+currentIndex);
						currentIndex++;
						if (head != null) {
							ReceivedSegment temp = head;
							while (temp != null) {
								 if(temp.index != currentIndex)
									 break;
								System.out.println("writing: "+temp.index);
								baos.write(temp.data.getBytes());
								head = head.next;
								temp = temp.next;
								currentIndex++;
							}
						}
					} else if (seqNumber > currentIndex) {
						System.out.println("out of order");
						ReceivedSegment recSeg = new ReceivedSegment(seqNumber, dataIn);
						if (head == null)
							head = recSeg;
						else {
							ReceivedSegment temp = head;
							ReceivedSegment prev = head;
							while (temp.next != null && temp.index < seqNumber) {
								prev = temp;
								temp = temp.next;
							}
							if (temp.index < seqNumber){
								temp.next = recSeg;
								System.out.println("inserting after: "+temp.index);
							}
							else {
								if(prev!=temp)
									prev.next = recSeg;
								else
									head = recSeg;
								recSeg.next = temp;
								System.out.println("inserting after: "+prev.index+" before: "+temp.index);
							}
						}
						System.out.println("Linked List:");
						ReceivedSegment t = head;
						while(t!=null)
						{
							System.out.print(t.index+" ");
							t = t.next;
						}
					}
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

	public static int generateChecksum(String s) {
		String hex_value = new String();
		int x, i, checksum = 0;
		for (i = 0; i < s.length() - 2; i = i + 2) {
			x = (int) (s.charAt(i));
			hex_value = Integer.toHexString(x);
			x = (int) (s.charAt(i + 1));
			hex_value = hex_value + Integer.toHexString(x);
			x = Integer.parseInt(hex_value, 16);
			checksum += x;
		}
		if (s.length() % 2 == 0) {
			x = (int) (s.charAt(i));
			hex_value = Integer.toHexString(x);
			x = (int) (s.charAt(i + 1));
			hex_value = hex_value + Integer.toHexString(x);
			x = Integer.parseInt(hex_value, 16);
		} else {
			x = (int) (s.charAt(i));
			hex_value = "00" + Integer.toHexString(x);
			x = Integer.parseInt(hex_value, 16);
		}
		checksum += x;
		hex_value = Integer.toHexString(checksum);
		if (hex_value.length() > 4) {
			int carry = Integer.parseInt(("" + hex_value.charAt(0)), 16);
			hex_value = hex_value.substring(1, 5);
			checksum = Integer.parseInt(hex_value, 16);
			checksum += carry;
		}
		checksum = generateComplement(checksum);
		return checksum;
	}

	public static int generateComplement(int checksum) {
		checksum = Integer.parseInt("FFFF", 16) - checksum;
		return checksum;
	}

	public static int receive(String s, int checksum) {
		int generated_checksum = generateChecksum(s);
		generated_checksum = generateComplement(generated_checksum);
		int syndrome = generated_checksum + checksum;
		syndrome = generateComplement(syndrome);
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
