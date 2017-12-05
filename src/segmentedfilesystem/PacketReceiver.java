package segmentedfilesystem;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class PacketReceiver {

	// FileTool documentation is in FIleTool.java
	FileTool file1 = new FileTool();
	FileTool file2 = new FileTool();
	FileTool file3 = new FileTool();
	
	// List of files that are available to be placed into lookUpTable
	ArrayList<FileTool> availableFiles = new ArrayList<>();
	
	// "lookupTable" is used so that we know which FileTool corresponds to which fileID from data
	HashMap<Byte, FileTool> lookUpTable  = new HashMap<>();
	
	// terminator will store the value of the maximum amount of packets from these 3 files
	int terminator = 0;

	/**
	 * Adds files to the list of the availableFiles
	 */
	public void setUpFiles(){
		availableFiles.add(file1);
		availableFiles.add(file2);
		availableFiles.add(file3);
	}

	/**
	 * This method manages packets as they are received
	 */
	public void startReceivingPackets(DatagramSocket socket) throws IOException{

		setUpFiles();
		
		boolean thisIsIt = false; // This boolean will be set to "true" once we know the total amount of packets that we are suppose to receive
		int lastPacketCounter = 0; // counts all "last" packets
		int counter  = 0; // Will be incremented every time we receive a packet

		while(true){
			byte[] buffer = new byte[1028];
			DatagramPacket packetToReceive = new DatagramPacket(buffer, buffer.length);
			socket.receive(packetToReceive);
			
			int status = checkStatus(packetToReceive.getData()); // Get status
			
			if(status == 1){
				handleHeaderPacket(packetToReceive);
				counter++;
			}else if(status == 2){
				handleUsualPacket(packetToReceive);
				counter++;
			}else if(status == 3){
				handleLastPacket(packetToReceive);
				counter++;
				lastPacketCounter++;	
			}else{
				System.out.println("An error has occured!");
				break;
			}
			
			if(lastPacketCounter == 3){ // If this condition is true then we know the maximum amount of packets that we are supposed to receive
				thisIsIt = true;
			}
			
			if(thisIsIt){
				if(counter == terminator){	// Once we know that we received all packets we break
					break;
				}
			}
		}
		
		socket.close();
		
		// This method is explained in FileTool.java
		file1.handlePackets();
		file2.handlePackets();
		file3.handlePackets();
	}
	
	/**
	 * Checks status of the packets
	 */
	public int checkStatus(byte[] data){
		if((data[0] & 1) == 0){
			return 1; // return 1 if dataPacket is the header
		}else{
			if(((data[0] >> 1) & 1) == 0){
				return 2; // return 2 if the data packet is usual data packet
			}else{
				return 3; // return 3 if the data packet is the last one
			}
		}

	}

	/**
	 *  Handles the header packet. Extracts the filename from this packet and stores it as a string
	 */
	public void handleHeaderPacket(DatagramPacket packetToReceive){
		byte fileID = packetToReceive.getData()[1];

		if(lookUpTable.containsKey(fileID)){
			byte[] fileNameData  = new byte[packetToReceive.getLength() - 2];
			fileNameData = Arrays.copyOfRange(packetToReceive.getData(), 2, packetToReceive.getLength());
			lookUpTable.get(fileID).setFileName(new String(fileNameData)); // Making the string out of byte array and set it in FileTool
		}else{
			lookUpTable.put(fileID, availableFiles.get(0));
			availableFiles.remove(0);
			byte[] fileNameData  = new byte[packetToReceive.getLength() - 2];
			fileNameData = Arrays.copyOfRange(packetToReceive.getData(), 2, packetToReceive.getLength());
			lookUpTable.get(fileID).setFileName(new String(fileNameData)); // Making the string out of byte array and set it in FileTool
		}
	}

	
	/**
	 * Handles the usual data packet.
	 * Adds it to the FileTool
	 */
	public void handleUsualPacket(DatagramPacket packetToReceive){
		byte fileID = packetToReceive.getData()[1];
		
		if(lookUpTable.containsKey(fileID)){
			lookUpTable.get(fileID).insert(packetToReceive.getData());
		}else{
			lookUpTable.put(fileID, availableFiles.get(0));
			availableFiles.remove(0);
			lookUpTable.get(fileID).insert(packetToReceive.getData());
		}

	}

	
	/**
	 * Handles the last packet.
	 * Trims the data since the buffer might not be full
	 * Extract the packet number, so that we know how many packets suppose to be received from this file
	 */
	public void handleLastPacket(DatagramPacket packetToReceive){
		byte fileID = packetToReceive.getData()[1];
		
		if(lookUpTable.containsKey(fileID)){
			int length = packetToReceive.getLength();
			byte[] data = new byte[length]; // Since the last packet might contain less that 1028 bytes we will trim the buffer to represent the actual size of received bytes
			data = Arrays.copyOfRange(packetToReceive.getData(), 0, length);
			lookUpTable.get(fileID).insert(data);
			int val = ((packetToReceive.getData()[2] & 0xff) << 8) | (packetToReceive.getData()[3] & 0xff);
			terminator += (val + 2);
		}else{
			lookUpTable.put(fileID, availableFiles.get(0));
			availableFiles.remove(0);
			int length = packetToReceive.getLength();
			byte[] data = new byte[length]; // Since the last packet might contain less that 1028 bytes we will trim the buffer to represent the actual size of received bytes
			data = Arrays.copyOfRange(packetToReceive.getData(), 0, length);
			lookUpTable.get(fileID).insert(data);
			int val = ((packetToReceive.getData()[2] & 0xff) << 8) | (packetToReceive.getData()[3] & 0xff);
			terminator += (val + 2);
		}

	}


}
