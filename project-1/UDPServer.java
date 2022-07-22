import java.io.*;
import java.net.*;
import java.util.*;
import java.text.MessageFormat;
import java.util.stream.Collectors;

// ---------------------------------------
// Server that receives HTTP GET requests.

class UDPServer {

	private static int socketPort = 9876;
	private static String nullByte = "\0";

	public static void main(String args[]) throws Exception {

		// -------------------------
		// Open socket at port 9876.

		System.out.println("Opening socket...");
		DatagramSocket serverSocket = new DatagramSocket(socketPort);
		byte[] receiveData = new byte[UDPPacket.PACKET_SIZE];
		System.out.println("DONE\n");

		// ------------------------------
		// Continuously process requests.

		while (true)
		{
			// ------------------------------
			// Receive packet sent by client.

			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);

			// ------------------------------------------------------
			// Get clients IP address, port number, and request data.

			InetAddress clientIP = receivePacket.getAddress();
			int clientPort = receivePacket.getPort();
			String requestData = new String(receivePacket.getData());
			System.out.println(MessageFormat.format("Packet received from client at {0}", clientIP));

			// ---------------------------------------
			// Extract file name and read into buffer.

			String fileName = requestData.split(" ")[1];
			System.out.println(MessageFormat.format("File requested : {0}\n", fileName));

			System.out.println("Reading file...");
			BufferedReader fileBuffer = new BufferedReader(new FileReader(fileName));
			String fileDataString = fileBuffer.lines().collect(Collectors.joining());
			fileBuffer.close();
			System.out.println("DONE\n");

			String response = "HTTP/1.0 200 TestFile.html Follows\r\n"
					+ "Content-Type: text/plain\r\n"
					+ "Content-Length: " + fileDataString.length() + "\r\n"
					+ "\r\n"
					+ fileDataString;

			// --------------------------
			// Segment file into packets.

			System.out.print("Segmenting response... ");
			ArrayList<UDPPacket> packetList = UDPPacket.segment(response.getBytes());
			System.out.print(packetList.size() + " packets created.\nDONE\n\n");

			// ------------------------------------
			// Sequentially send packets to client.

			System.out.print("Sending packets");
			for (UDPPacket packet : packetList) {
				DatagramPacket sendPacket = packet.getDatagramPacket(clientIP, clientPort);
				serverSocket.send(sendPacket);
				System.out.print(".");
			}

			//serverSocket.send(new DatagramPacket(nullByte.getBytes(), nullByte.getBytes().length, clientIP, clientPort));
			//serverSocket.send(new UDPPacket().getDatagramPacket(clientIP, clientPort));

			ArrayList<UDPPacket> nullPacket = UDPPacket.segment(nullByte.getBytes());
			DatagramPacket nullDatagram = nullPacket.get(0).getDatagramPacket(clientIP, clientPort);
			serverSocket.send(nullDatagram);

			System.out.println("\nDONE\n");
		}
	}
}