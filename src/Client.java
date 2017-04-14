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
		//inputs
		Scanner scan = new Scanner(System.in);
		String hostname = scan.next();
		int port = scan.nextInt();
		String filename = scan.next();
		int N = scan.nextInt();
		int mss = scan.nextInt();
		scan.close();
		
		//Socket
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress serverIP = InetAddress.getByName(hostname);
		
		Path filePath = Paths.get(filename);
		byte[] dataPacket = Files.readAllBytes(filePath);
		ArrayList<byte[]> segmentList = chunksDivision(dataPacket, mss);
		
		byte[] send = null;
		byte[] receive = null;
		DatagramPacket toReceiver = new DatagramPacket(send, send.length, serverIP, port);
		clientSocket.send(toReceiver);
		DatagramPacket fromReceiver = new DatagramPacket(receive, receive.length);
		clientSocket.receive(fromReceiver);
		int seqAck = ackHandler(fromReceiver.getData());
		if(seqAck!=-1){
			
		}
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
		String checksum = checksumCalculation(data);
		String fixedVal = "0101010101010101"; 
		return sequenceStr + checksum + fixedVal;
	}
	public static String checksumCalculation(byte[] data){
		int dataLength = data.length;
		int Checksum = 0;
		int i = 0;
		while(dataLength>0)
		{
		    int Word = ((data[i]<<8) + data[i+1]) + Checksum;

		    Checksum = Word & 0x0FFFF;

		    Word = (Word>>16);

		    Checksum = Word + Checksum;

		    dataLength -= 2;
		    i += 2;
		}
		Checksum = ~Checksum;
		return Integer.toString(Checksum);
	}
	public static int ackHandler(byte[] data){
		String ACK = Arrays.toString(data);
		String packetType = ACK.substring(48, 64);
		if(packetType.equals("1010101010101010")){
			return binToDec(ACK.substring(0, 32));
		}
		return -1;
	}
	private static int binToDec(String substring) {
		int dec = 0;
		int power = 0;
		for(int i=substring.length()-1; i>=0; i--){
			if(substring.charAt(i)=='1')
				dec += Math.pow(2, power);
			power++;
		}
		return dec;
	}
}
