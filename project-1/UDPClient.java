import java.net.*;
import java.util.*;
import java.text.MessageFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

// ----------------------------------------
// UDP client that sends HTTP GET requests.

class UDPClient {

	public static void main(String args[]) throws Exception {

		// -----------------------------------
		// Open socket and initialize request.

		System.out.println("Opening socket...");
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress serverIP = InetAddress.getByName("localhost");
		int serverPort = 9876;
		String httpRequest = "GET TestFile.html HTTP/1.0";
		System.out.println("DONE\n");

		// ---------------------------
		// Send GET request to server.

		System.out.println("Sending request to server...");
		byte[] sendData = httpRequest.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverIP, serverPort);
		clientSocket.send(sendPacket);
		System.out.println("DONE\n");

		// ----------------------------
		// Receive packets from server.

		System.out.println("Receiving response from server...");
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		int packetNum = 0;
		boolean isFinished = false;
		ArrayList<UDPPacket> receivedPacketList = new ArrayList<>();

		while (!isFinished)
		{
			clientSocket.receive(receivePacket);
			UDPPacket dataReceived = UDPPacket.makePacket(receivePacket);
			packetNum++;

			if (dataReceived.getPacketData()[0] == '\0')
			{
				isFinished = true;
			}
			else
			{
				receivedPacketList.add(dataReceived);
				System.out.println("Received Packet : " + packetNum);
			}
		}
		System.out.println("DONE\n");

		clientSocket.close();

		// -----------------------------
		// Artificially corrupt packets.

		System.out.print("Running Gremlin Function...");
		String errorProbability = "0.0";
		if (args.length > 0) {
			errorProbability = args[0];
			System.out.println(MessageFormat.format(" Error probability : {0}.", errorProbability));
		} else {
			System.out.println(" No error probability value was supplied. Defaulting to 0.");
		}

		for (UDPPacket packet : receivedPacketList) {
			gremlin(errorProbability, packet);
		}
		System.out.println("DONE\n");

		// ------------------------------------
		// Run error detection and re-assembly.

		System.out.println("Checking for errors...");
		errorDetection(receivedPacketList);
		System.out.println("DONE\n");

		System.out.println("Re-assembling packets...");
		byte[] reassembledFile = UDPPacket.reassemble(receivedPacketList);
		String reassembledFileString = new String(reassembledFile);
		System.out.println("DONE\n");

		Document doc = Jsoup.parse(reassembledFileString);
		System.out.println(MessageFormat.format("File received from server: \n{0}", doc.toString()));
	}

	private static void errorDetection(ArrayList<UDPPacket> packetList) {
		for (UDPPacket packet : packetList) {
			String checksumHeaderValue = packet.getHeaderValue(UDPPacket.HEADER_VALUES.CHECKSUM);
			Short checkSum = Short.parseShort(checksumHeaderValue);
			byte[] data = packet.getPacketData();
			short calculatedCheckSum = UDPPacket.calculateChecksum(data);
			if (!checkSum.equals(calculatedCheckSum)) {
				String segmentHeaderValue = packet.getHeaderValue(UDPPacket.HEADER_VALUES.SEGMENT_NUM);
				System.out.println("Error detected in UDPPacket Number: " + segmentHeaderValue);
			}
		}
	}

	private static void gremlin(String probability, UDPPacket packet) {
		Random rand = new Random();
		double damageProb = rand.nextDouble();
		double flipProb = rand.nextDouble();
		int bytesToChange;

		if (flipProb <= 0.5) {
			bytesToChange = 1;
		} else if (flipProb <= 0.8) {
			bytesToChange = 2;
		} else {
			bytesToChange = 3;
		}

		if (Double.parseDouble(probability) >= damageProb) {
			for (int i = 0; i < bytesToChange; i++) {
				byte[] data = packet.getPacketData();
				int byteNum = rand.nextInt(packet.getPacketSize());
				data[byteNum] = (byte) ~data[byteNum];
			}
		}
	}
}