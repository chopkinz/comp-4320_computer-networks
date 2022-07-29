import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
/*
    Matthew Bentz, Chase Hopkins, James Haberstroh
*/

class UDPServer {
    public static void main(String[] args) throws Exception {
        // port number
        DatagramSocket serverSocket = new DatagramSocket(8080);

        byte[] receiveData = new byte[1024];
        String nullByte = "\0";
        String errorCode = "";
        int sequenceNumber = 0;
        List<byte[]> packets = new ArrayList<byte[]>();
        final int WINDOW_SIZE = 8;

        while (true) {
            // Receiving datagram from client
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);

            // Get IP address and port #
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();

            String sentence = new String(receivePacket.getData());
            System.out.println("Data received is: " + sentence);

            // get the file name out of the sentence above
            String[] arr = sentence.split(" ");
            String fileNameRequested = arr[1];

            // create bufferedReader to read in the file
            // 1 character equals 2 bytes.
            try {
                BufferedReader fileIn = new BufferedReader(new FileReader(fileNameRequested));
                StringBuilder fileData = new StringBuilder();
                String temp = fileIn.readLine();
                System.out.println("line: " + temp);
                while (temp != null) {
                    System.out.println(temp);
                    fileData.append(temp);
                    temp = fileIn.readLine();
                }
                fileIn.close();
            } catch (FileNotFoundException e) {
                // catch exception if file is not found, send error html file.
                System.err.println("File not found. Program exiting. ");
                errorCode = "404 - File Not Found";
            }

            if (errorCode.isEmpty()) {
                errorCode = "200 - Document follows";
            }

            // converting file to byte array
            byte[] bytes = null;
            try {
                bytes = Files.readAllBytes(Paths.get(fileNameRequested));
            } catch (NoSuchFileException e) {
                bytes = Files.readAllBytes(Paths.get("errorFile.html"));
            }

            // Sending HTTPHeader:
            String HTTPHeader = "\n" + arr[2] + " " + errorCode + "\r\nContent-Type: text/plain\r\nContent-Length: "
                    + bytes.length + " bytes\r\n";

            byte[] headerPacket = HTTPHeader.getBytes();
            // create a datagram with data to send, length, IP addr, port
            DatagramPacket sendedPacket = new DatagramPacket(headerPacket, headerPacket.length, IPAddress, port);
            // send datagram to the server
            serverSocket.send(sendedPacket);

            boolean finished = false;
            int start = 0;
            int end = 1024;
            if (end > headerPacket.length) {
                end = headerPacket.length;
            }

            // sending header
            while (finished == false) {
                byte[] temp = new byte[1024];
                int dataIndex = 0;
                for (int i = start; i < end; i++) {

                    temp[dataIndex] = headerPacket[i];
                    dataIndex++;
                    if (i == headerPacket.length - 1) {
                        finished = true;
                    }
                }
                start = end;
                if (end + 1024 < headerPacket.length) {
                    end += 1024;
                } else {
                    end = headerPacket.length;
                }
                // create a datagram with data to send, length, IP addr, port
                DatagramPacket sendPacket = new DatagramPacket(temp, temp.length, IPAddress, port);
                // send datagram to the server
                serverSocket.send(sendPacket);
            }

            finished = false;
            start = 0;
            end = 1024;
            if (end > bytes.length) {
                end = bytes.length;
            }

            // splitting up file into packets
            System.out.println("\n\n---------------------------\nPackets in circulation\n---------------------------");
            while (finished == false) {
                byte[] data = new byte[1025];

                int dataIndex = 0;
                for (int i = start; i < end; i++) {

                    data[dataIndex] = bytes[i];
                    dataIndex++;
                    if (i == bytes.length - 1) {
                        finished = true;
                    }
                }
                start = end;
                if (end + 1024 < bytes.length) {
                    end += 1024;
                } else {
                    end = bytes.length;
                }

                data[1024] = (byte) sequenceNumber;
                sequenceNumber++;
                System.out.println("SEQUENCE NUMBER = " + data[1024]);

                packets.add(data);
            }

            int startIndex = 0;
            int endIndex = 7;
            ArrayList<Integer> packetsToResend = new ArrayList<Integer>();
            boolean Sending = true;
            // sending packets, waiting for acks and nacks and re-sending packets.
            while (Sending) {
                byte[] temp = packets.get(startIndex);

                // create a datagram with data to send, length, IP addr, port
                DatagramPacket sendPacket = new DatagramPacket(temp, temp.length, IPAddress, port);
                // send datagram to the server
                serverSocket.send(sendPacket);

                // receiving ack or nack
                DatagramPacket ackOrNackPacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(ackOrNackPacket);

                String X = new String(ackOrNackPacket.getData());
                String[] input = X.split(" ");
                String Y = input[1];
                int Z = Character.getNumericValue(Y.charAt(0));
                // System.out.println("sequence Number: " + Z);
                String ackOrNack = input[0];
                int nackIndex = Character.getNumericValue(Y.charAt(0));

                // checkif it is ack or nack
                if (ackOrNack.compareTo("nack") == 0) {
                    packetsToResend.add(nackIndex);
                    startIndex++;
                } else {
                    startIndex++;
                    endIndex++;
                }

                // exit the while loop at the last packet
                if (startIndex == packets.size()) {
                    break;
                }
            }

            // resending NACK packages
            for (int X : packetsToResend) {
                byte[] temp = packets.get(X);

                // create a datagram with data to send, length, IP addr, port
                DatagramPacket sendPacket = new DatagramPacket(temp, temp.length, IPAddress, port);
                // send datagram to the server
                serverSocket.send(sendPacket);
            }

            // Sending the last nullbyte packet
            byte[] nullBytePacket = nullByte.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(nullBytePacket, nullBytePacket.length, IPAddress, port);
            serverSocket.send(sendPacket);
            System.out.println("\nThe following have been resent due to NACK:\n" + packetsToResend);

            System.out.println(
                    "----------------------------------\nnullByte packet sent.\n----------------------------------");

            System.exit(0);
        }
    }
}
