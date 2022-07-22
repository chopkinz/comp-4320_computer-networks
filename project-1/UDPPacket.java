import java.net.*;
import java.nio.*;
import java.util.*;

// -----------------------------------------------------------
// Packet helper class to initialize packets and segment data.

public class UDPPacket {
	private byte[] packetData;
	public static int PACKET_SIZE = 1024;
	private Map<String, String> packetHeader;
	private static final int HEADER_LINES = 4;
	private static final String HEADER_CHECK_SUM = "HeaderCheckSum";
	private static final String HEADER_SEQUENCE_NUM = "HeaderSequence";
	private static final int PACKET_DATA_SIZE = PACKET_SIZE - HEADER_LINES;
	public enum HEADER_VALUES {
		SEGMENT_NUM,
		CHECKSUM
	}

	public UDPPacket() {
		packetHeader = new HashMap<>();
		packetData = new byte[PACKET_SIZE];
	}

	static UDPPacket makeUDPPacket(DatagramPacket packet) {

		// ----------------------------------
		// Initialize UDPPacket return value.

		UDPPacket newPacket = new UDPPacket();
		ByteBuffer bytebuffer = ByteBuffer.wrap(packet.getData());
		byte[] data = packet.getData();
		byte[] remainder;

		// --------------------
		// Set header segments.

		newPacket.setHeaderValue(HEADER_VALUES.SEGMENT_NUM, Short.toString(bytebuffer.getShort()));
		newPacket.setHeaderValue(HEADER_VALUES.CHECKSUM, Short.toString(bytebuffer.getShort()));

		// ----------------
		// Set packet data.

		remainder = new byte[data.length - bytebuffer.position()];
		System.arraycopy(data, bytebuffer.position(), remainder, 0, remainder.length);
		newPacket.setPacketData(remainder);
		return newPacket;
	}

	static ArrayList<UDPPacket> segment(byte[] response) {

		ArrayList<UDPPacket> packetList = new ArrayList<>();
		int len = response.length;
		int segmentCounter = 0;
		int byteCounter = 0;

		// ----------------------------------------
		// Segment response into 1024 byte packets.

		while (byteCounter < len) {

			UDPPacket upcomingPacket = new UDPPacket();
			byte[] data = new byte[PACKET_DATA_SIZE];
			int dataSize = PACKET_DATA_SIZE;

			if (len - byteCounter < PACKET_DATA_SIZE) {
				dataSize = len - byteCounter;
			}

			int j = byteCounter;
			for (int i = 0; i < dataSize; i++) {
				data[i] = response[j];
				j++;
			}

			// --------------------------------
			// Set segment and checksum values.

			upcomingPacket.setPacketData(data);
			upcomingPacket.setHeaderValue(HEADER_VALUES.SEGMENT_NUM, Integer.toString(segmentCounter));
			String checkSumValue = String.valueOf(UDPPacket.calculateChecksum(data));
			upcomingPacket.setHeaderValue(HEADER_VALUES.CHECKSUM, checkSumValue);
			packetList.add(upcomingPacket);

			segmentCounter++;
			byteCounter += dataSize;
		}

		return packetList;
	}

	static byte[] reassemble(ArrayList<UDPPacket> packetList) {

		int size = 0;
		int counter = 0;
		byte[] assembledPacket;

		// ---------------------------------
		// Allocate byte array for response.

		for (UDPPacket packet : packetList) {
			size += packet.getPacketSize();
		}
		assembledPacket = new byte[size];

		// ----------------------------------------
		// Add all packetList data to return value.

		for (int i = 0; i < packetList.size(); i++) {

			for (UDPPacket packet : packetList) {

				String segment = packet.getHeaderValue(HEADER_VALUES.SEGMENT_NUM);

				if (Integer.parseInt(segment) == i) {

					for (int j = 0; j < packet.getPacketSize(); j++) {

						assembledPacket[counter + j] = packet.getPacketData(j);
					}

					counter += packet.getPacketSize();
					break;
				}
			}
		}

		return assembledPacket;
	}

	static short calculateChecksum(byte[] packet) {

		// ------------------
		// Absolute wizardry.

		long sum = 0;
		int count = 0;
		int byteLength = packet.length;

		while (byteLength > 1) {
			sum += ((packet[count]) << 8 & 0xFF00) | ((packet[count + 1]) & 0x00FF);
			if ((sum & 0xFFFF0000) > 0) {
				sum = ((sum & 0xFFFF) + 1);
			}
			count += 2;
			byteLength -= 2;
		}
		if (byteLength > 0) {
			sum += (packet[count] << 8 & 0xFF00);
			if ((sum & 0xFFFF0000) > 0) {
				sum = ((sum & 0xFFFF) + 1);
			}
		}
		return (short) (~sum & 0xFFFF);
	}

	String getHeaderValue(HEADER_VALUES headerValue) {
		switch (headerValue) {
			case SEGMENT_NUM:
				return packetHeader.get(HEADER_SEQUENCE_NUM);
			case CHECKSUM:
				return packetHeader.get(HEADER_CHECK_SUM);
			default:
				throw new IllegalArgumentException("Error in getHeaderValue");
		}
	}

	private void setHeaderValue(HEADER_VALUES headerValue, String value) {
		switch (headerValue) {
			case SEGMENT_NUM:
				packetHeader.put(HEADER_SEQUENCE_NUM, value);
				break;
			case CHECKSUM:
				packetHeader.put(HEADER_CHECK_SUM, value);
				break;
			default:
				throw new IllegalArgumentException("Error in setHeaderValue");
		}
	}

	private byte getPacketData(int index) {
		if (index >= 0 && index < packetData.length) {
			return packetData[index];
		}
		throw new IndexOutOfBoundsException("getPacketData out of bound exception at index " + index);
	}

	byte[] getPacketData() {
		return packetData;
	}

	int getPacketSize() {
		return packetData.length;
	}

	DatagramPacket getDatagramPacket(InetAddress ip, int port) {
		byte[] setData = ByteBuffer.allocate(PACKET_SIZE)
				.putShort(Short.parseShort(packetHeader.get(HEADER_SEQUENCE_NUM)))
				.putShort(Short.parseShort(packetHeader.get(HEADER_CHECK_SUM)))
				.put(packetData)
				.array();

		return new DatagramPacket(setData, setData.length, ip, port);
	}

	private void setPacketData(byte[] toSet) throws IllegalArgumentException {
		int toSetLen = toSet.length;

		if (toSetLen > 0) {
			packetData = new byte[toSetLen];
			System.arraycopy(toSet, 0, packetData, 0, packetData.length);
		} else {
			throw new IllegalArgumentException(
					"Illegal argument exception in setPacketData: toSet.length = " + toSet.length);
		}
	}
}