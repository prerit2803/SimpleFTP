import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
	
	public static void main(String[] args) throws SocketException, IOException {
		// TODO Auto-generated method stub
		Scanner scan = new Scanner(System.in);
		String hostname = scan.next();
		int port = scan.nextInt();
		String filename = scan.next();
		int N = scan.nextInt();
		int mss = scan.nextInt();
		scan.close();
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress serverIP = InetAddress.getByName(hostname);
		
		Path filePath = Paths.get(filename);
		byte[] dataPacket = Files.readAllBytes(filePath);
		ArrayList<byte[]> segmentList = chunksDivision(dataPacket, mss);
		
		System.out.println("Sending");
		DatagramPacket toReceiver = new DatagramPacket(dataPacket, dataPacket.length, serverIP, port);
		clientSocket.send(toReceiver);
	}
	public static ArrayList<byte[]> chunksDivision(byte[] dataPacket, int MSS){
		ArrayList<byte[]> segmentList = new ArrayList<byte[]>();
		int totalPackets = dataPacket.length / MSS;
		for(int i=0; i< totalPackets; i++){
			byte[] segment = new byte[MSS];
			segment = Arrays.copyOfRange(dataPacket, MSS * i, MSS * (i+1));
			segmentList.add(segment);
		}
		return segmentList;
	}
	public static String createHeader(int sequence, byte[] data){
		String sequenceStr = Integer.toBinaryString(sequence);
		
		String fixedVal = "0101010101010101"; 
		return null;
	}
}