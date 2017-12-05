package segmentedfilesystem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class Main {


	public static void main(String[] args) throws IOException {
		int port = 6014;
		InetAddress address = InetAddress.getByName("146.57.33.55");

		DatagramSocket socket = new DatagramSocket();

		//Say hello to server
		byte[] bufferToSend = new byte[1028];
		DatagramPacket packetToSend = new DatagramPacket(bufferToSend, bufferToSend.length, address, port);
		socket.send(packetToSend);
		
		PacketReceiver receiver = new PacketReceiver();
	
		receiver.startReceivingPackets(socket);	
	}
}
