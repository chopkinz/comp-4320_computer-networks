// ----------------------------------------------
// Matthew Bentz, Chase Hopkins, James Haberstroh

import java.net.*;
import java.util.*;
import java.text.MessageFormat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

// ----------------------------------------
// UDP client that sends HTTP GET requests.

class UDPClient {

	private static int serverPort = 9876;
	private static String serverName = "192.168.56.1";//"localhost";

	public static void main(String args[]) throws Exception {

		// -----------------------------------
		// Open socket and initialize request.

		System.out.println("Opening socket...");
		DatagramSocket clientSocket = new DatagramSocket();
		String httpRequest = "GET TestFile.html HTTP/1.0";
		System.out.println("DONE\n");

		// ---------------------------
		// Send GET request to server.

		System.out.println("Sending request to server...");
		byte[] sendData = httpRequest.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(serverName), serverPort);
		clientSocket.send(sendPacket);
		System.out.println("DONE\n");

		// ----------------------------
		// Receive packets from server.

		System.out.println("Receiving response from server...");
		byte[] receiveData = new byte[UDPPacket.PACKET_SIZE];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		int packetNum = 0;
		boolean isFinished = false;
		ArrayList<UDPPacket> receivedPacketList = new ArrayList<>();

		while (!isFinished)
		{
			clientSocket.receive(receivePacket);
			UDPPacket dataReceived = UDPPacket.makeUDPPacket(receivePacket);
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
		String errorProbability = "0";

		if (args.length > 0) {
			errorProbability = args[0];
			System.out.println(MessageFormat.format(" Error probability : {0}.", errorProbability));
		} else {
			System.out.println(" No error probability value was supplied. Defaulting to 0.");
		}

		if (Double.parseDouble(errorProbability) > 0) {
			gremlin(errorProbability, receivedPacketList);
		}
		System.out.println("DONE\n");

		// ------------------------------------
		// Run error detection and re-assembly.

		System.out.println("Checking for errors...");
		detectErrors(receivedPacketList);
		System.out.println("DONE\n");

		System.out.println("Re-assembling packets...");
		byte[] reassembledFile = UDPPacket.reassemble(receivedPacketList);
		String reassembledFileString = new String(reassembledFile);
		System.out.println("DONE\n");

		Document doc = Jsoup.parse(reassembledFileString);
		System.out.println(MessageFormat.format("File received from server: \n{0}", doc.toString()));
	}

	private static void detectErrors(ArrayList<UDPPacket> packetList) {
		for (UDPPacket packet : packetList) {

			// ------------------------------------
			// Retrieve checksum value from packet.

			String checksumHeaderValue = packet.getHeaderValue(UDPPacket.HEADER_VALUES.CHECKSUM);
			Short packetCheckSum = Short.parseShort(checksumHeaderValue);

			// -----------------------------------------------
			// Compare packet checksum to calculated checksum.

			byte[] data = packet.getPacketData();
			short calculatedCheckSum = UDPPacket.calculateChecksum(data);

			if (!packetCheckSum.equals(calculatedCheckSum)) {
				String segmentHeaderValue = packet.getHeaderValue(UDPPacket.HEADER_VALUES.SEGMENT_NUM);
				System.out.println(MessageFormat.format("Error detected in packet : {0}", segmentHeaderValue));
			}
		}
	}

	private static void gremlin(String probability, ArrayList<UDPPacket> packetList) {
		for (UDPPacket packet : packetList) {

			Random rand = new Random();
			double damageProbability = rand.nextDouble();
			double byteAmountProbability = rand.nextDouble();
			int bytesToChange;

			// ----------------------------------------
			// Determine the amount of bytes to change.

			if (byteAmountProbability <= 0.5) {
				bytesToChange = 1;
			} else if (byteAmountProbability <= 0.8) {
				bytesToChange = 2;
			} else {
				bytesToChange = 3;
			}

			// ----------------------------------------
			// Determine if the packet will be damaged.

			if (Double.parseDouble(probability) >= damageProbability) {
				for (int i = 0; i < bytesToChange; i++) {
					byte[] packetData = packet.getPacketData();
					int byteNum = rand.nextInt(packet.getPacketSize());
					packetData[byteNum] = (byte) ~packetData[byteNum];
				}
			}
		}
	}
}