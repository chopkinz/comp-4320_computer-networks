package server;
import java.net.DatagramPacket;
public interface ISegmentation {
	// accepts data in bytes and converts into packets equal in size
	DatagramPacket[] segmentPackets(byte[] data, int packetSize);
	int calculateChecksum(byte[] buf);
	public byte[] includeHeaderLines(byte[] buf, int sequenceNumber);
}