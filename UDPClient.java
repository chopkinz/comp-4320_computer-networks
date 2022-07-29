import java.io.*;
import java.net.*;
import java.lang.Math;
import java.util.*;

/*
    Matthew Bentz, Chase Hopkins, James Haberstroh
*/

class UDPClient {
	public static void main(String[] args) throws Exception {

		// variables
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1025];
		String request = "";
		String[] checkup;
		String modifiedSentence = "";
		int sequenceNumber = 0;
		int corruptedCheckSum = 0;
		Stack<Integer> SQstack = new Stack<>();

		// create input stream
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		InetAddress IPAddress = null;

		// create client socket
		DatagramSocket clientSocket = new DatagramSocket();

		// Promt user for IP address of the server
		System.out.println("Enter server IPAdress to connect to.");
		String ip = inFromUser.readLine();
		try {
			// Translate hostname to IP address using DNS
			IPAddress = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			System.out.println("invalid IPAddress! Please try again with a valid IP.");
		}

		// prompt user for the gremlin probability
		double corruptionProb;
		double lossProb;
		do {
			System.out.println("Enter your gremlin probability for packet corruption: ");
			corruptionProb = Double.parseDouble(inFromUser.readLine());
			System.out.println(
					"Enter your gremlin probability for packet loss (corruption prob + loss prob) should be less than one: ");
			lossProb = Double.parseDouble(inFromUser.readLine());
			if (corruptionProb + lossProb > 1) {
				System.out.println("Please re-enter probabilities that total less than 1.");
			} else {
				break;
			}
		} while (true);

		// prompt user for the HTTP request
		do {
			System.out.println("Enter an HTTP GET request.");
			request = inFromUser.readLine();
			checkup = request.split(" ");
		} while (checkup.length != 3);

		// convert the string into a byte array
		sendData = request.getBytes();

		// create a datagram with data to send, length, IP addr, port
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8080);

		// send datagram to the server (this is the HTTP request)
		clientSocket.send(sendPacket);

		// Read the datagram from the server till nullbyte packet is received
		boolean finished = false;
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		int count = 0;
		ArrayList<Integer> corruptedPackets = new ArrayList<>();
		ArrayList<Integer> lostPackets = new ArrayList<>();

		while (finished == false) {
			clientSocket.receive(receivePacket);
			if (receivePacket.getData()[0] == (byte) '\u0000')
				break;

			if (count != 0 && count <= 2) {
				// cutting off the sequence number
				byte[] temp = Arrays.copyOf(receivePacket.getData(), 1024);

				modifiedSentence = new String(temp);
				System.out.print(modifiedSentence);
			}

			else if (count > 2) {
				sequenceNumber = (int) receivePacket.getData()[1024];
				SQstack.push(sequenceNumber);

				// Extracting sequence number from packet sequence number
				byte[] temp = Arrays.copyOf(receivePacket.getData(), 1024);
				modifiedSentence = new String(temp);
				System.out.print(modifiedSentence);
				// System.out.print("SEQUENCENUMBER: " + sequenceNumber);
				byte[] uncorrupted = receivePacket.getData();
				int uncorruptedChecksum = calcChecksum(uncorrupted);
				// GREMLIN MUAHAHAHHHA
				byte[] corrupted = Gremlin(corruptionProb, lossProb, uncorrupted);
				if (corrupted != null) {
					corruptedCheckSum = calcChecksum(corrupted);
				}
				// check if packet is lost, if so send NACK
				if (corrupted == null) {
					lostPackets.add(sequenceNumber);
					String nack = "nack " + sequenceNumber;
					sendData = nack.getBytes();
					DatagramPacket nackPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8080);
					clientSocket.send(nackPacket);

					// check if packet is corrupted, if so send NACK
				} else if (corruptedCheckSum != uncorruptedChecksum) {
					corruptedPackets.add(sequenceNumber);
					String nack = "nack " + sequenceNumber;
					// convert the string into a byte array
					sendData = nack.getBytes();
					DatagramPacket nackPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8080);
					clientSocket.send(nackPacket);

					// package is not corrupted or lost, send ACK
				} else {
					String ack = "ack " + sequenceNumber;
					// convert the string into a byte array
					sendData = ack.getBytes();
					DatagramPacket ackPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 8080);
					clientSocket.send(ackPacket);
				}
			}

			count++;
		}

		System.out.println(
				"\n-----------------------------------\nPacket Transmission Report\n-----------------------------------");
		if (corruptedPackets.isEmpty()) {
			System.out.println("No packets were corrupted");
		} else {
			System.out.println("The packets with the following seq. numbers were corrupted: \n" + corruptedPackets);
		}
		System.out.println("-----------------------------------");
		if (lostPackets.isEmpty()) {
			System.out.println("No packets were lost");
		} else {
			System.out.println("The packets with the following seq. numbers were lost: \n" + lostPackets);
			System.out.println("server did re-send packets that were lost\n");
		}
		System.out.println("-----------------------------------");
		System.out.println("sequence numbers received: " + SQstack);

		clientSocket.close();
	}

	// gremlin function to corrupt bytes in packets
	public static byte[] Gremlin(double p, double loss, byte[] pack) {
		// this is for combining corruption and loss
		// c is for corrupt
		double c = Math.random();
		if (c < p) {
			double x = Math.random();
			// 1 byte corrupted
			if (x <= 0.5) {
				Random rand = new Random();
				int z = rand.nextInt(1024);
				pack[z] /= 2;
			}
			// 2 bytes corrupted
			else if (x <= 0.8) {
				Random rand = new Random();
				int z = rand.nextInt(1024);
				int y = rand.nextInt(1024);
				if (x == y) {
					y = rand.nextInt(1024);
				}
				pack[z] /= 2;
				pack[y] /= 2;
			}
			// 3 bytes corrupted
			else {
				Random rand = new Random();
				int z = rand.nextInt(1024);
				int y = rand.nextInt(1024);
				int q = rand.nextInt(1024);
				if (x == y || x == q || y == q) {
					y = rand.nextInt(1024);
					q = rand.nextInt(1024);
				}
				pack[z] /= 2;
				pack[y] /= 2;
				pack[q] /= 2;
			}
		}

		// packet loss and corruption do not happen at the same time
		// if packet is lost, we set it to null
		else if (c < p + loss) {
			pack = null;
		}
		return pack;
	}

	/* Calculates the checksum */
	public static int calcChecksum(byte[] data) {
		int calc_checksum = 0;
		for (int i = 0; i < data.length; i++) {
			calc_checksum += data[i];
		}
		return calc_checksum;
	}

}