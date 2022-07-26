package client;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public class AssemblerImpl implements IAssembler {

	public ArrayList<DatagramPacket> receivedPackets;
	private Comparator<DatagramPacket> packetComparator;
	private DatagramPacket nullPacket;

	AssemblerImpl() {
		this.receivedPackets = new ArrayList<DatagramPacket>();
		this.packetComparator = new PacketComparator();
	}

	public void newPacketIn(DatagramPacket newPacket) {
		if (!receivedPackets.contains(newPacket)) {
			System.out.println("NEW PACKET IN: " + SRPacket.parseSequenceNumber(newPacket));
			if (SRPacket.parseChecksum(newPacket) == 0) { // null packet (final packet) received
				System.out.println("received null packet");
				this.nullPacket = newPacket;
			}
			this.receivedPackets.add(newPacket);
		}
	}

	public byte[] getAssembledDocument() {
		String document = "";
		Collections.sort(receivedPackets, new SRPacketComparator());
		for (DatagramPacket packet : receivedPackets) {
			document += new String(SRPacket.getData(packet));
			// System.out.println(SRPacket.getData(packet));
		}
		return document.getBytes();
	}

	public boolean isComplete() {
		if (nullPacket != null) {
			SRPacket nullSR = new SRPacket(nullPacket);
			System.out.println("Null packet seq: " + nullSR.calculateOriginalSequenceNumber());
			System.out.println("received packets length: " + receivedPackets.size());
			return nullSR.calculateOriginalSequenceNumber() == receivedPackets.size() - 1;
		}
		return false;
	}
}
