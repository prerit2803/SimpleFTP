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
import java.net.SocketException;
import java.util.Scanner;

public class Server {

	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub
		Scanner scan = new Scanner(System.in);
		int port = scan.nextInt();
		String filename = scan.next();
		double pos = scan.nextDouble();
		scan.close();
		DatagramSocket serverSocket = new DatagramSocket(port);
		byte[] dataPacket = new byte[1024];
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DatagramPacket fromClient = null;
		boolean flag = true;
		while (flag) {
			try {
				System.out.println("Receiving");
				fromClient = new DatagramPacket(dataPacket, dataPacket.length);
				serverSocket.receive(fromClient);
				baos.write(fromClient.getData());
			} catch (Exception e) {
				flag = false;
			}
		}
		System.out.println("Writing to file");
		FileOutputStream file = new FileOutputStream(filename);
		baos.writeTo(file);
		file.close();
		serverSocket.close();
	}

}
