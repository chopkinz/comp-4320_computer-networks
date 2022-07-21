import java.io.*;
import java.net.*;
import java.util.*;
import java.text.MessageFormat;

// ---------------------------------------
// Server that receives HTTP GET requests.

class UDPServer {
	public static void main(String args[]) throws Exception {

		// -------------------------
		// Open socket at port 9876.

		System.out.println("Opening socket...");
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receiveData = new byte[1024];
		String nullByte = "\0";
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
			System.out.println(MessageFormat.format("Packet received from client at {0}:{1}", clientIP, clientPort));

			// ---------------------------------------
			// Extract file name and read into buffer.

			String fileName = requestData.split(" ")[1];
			System.out.println(MessageFormat.format("File requested : {0}\n", fileName));

			BufferedReader fileIn = new BufferedReader(new FileReader(fileName));
			StringBuilder fileDataContents = new StringBuilder();

			String line = fileIn.readLine();
			System.out.println("line: " + line);
			while (line != null) {
				//System.out.println(line);
				fileDataContents.append(line);
				line = fileIn.readLine();
			}
			fileIn.close();

			String httpHeader = "HTTP/1.0 TestFile.html Follows\r\n"
					+ "Content-Type: text/plain\r\n"
					+ "Content-Length: " + fileDataContents.length() + "\r\n"
					+ "\r\n" + fileDataContents;

			ArrayList<UDPPacket> packetList = UDPPacket.segment(httpHeader.getBytes()); // segments file into packets
			System.out.println("List of segmented packets is " + packetList.size() + " packets long");

			for (UDPPacket packet : packetList) {
				DatagramPacket sendPacket = packet.getDatagramPacket(clientIP, clientPort);
				serverSocket.send(sendPacket);
			}

			// Notify the client that all data has been sent via a null character
			System.out.println("Sending null character");
			ArrayList<UDPPacket> nullPacket = UDPPacket.segment(nullByte.getBytes());
			DatagramPacket nullDatagram = nullPacket.get(0).getDatagramPacket(clientIP, clientPort);
			serverSocket.send(nullDatagram);
			System.out.print("Sent");
		}
	}
}