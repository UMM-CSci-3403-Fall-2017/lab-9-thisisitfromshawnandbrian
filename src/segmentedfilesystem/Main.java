package segmentedfilesystem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;


public class Main {


	public static void main(String[] args) throws IOException {
		int port = 6014;
		InetAddress address = InetAddress.getByName("146.57.33.55");

		DatagramSocket socket = new DatagramSocket();

		//Say hello to server
		byte[] bufferToSend = new byte[1028];
		DatagramPacket packetToSend = new DatagramPacket(bufferToSend, bufferToSend.length, address, port);
		socket.send(packetToSend);


		//Prepare for answer
		HashMap<Byte, Integer> lookUpTable = new HashMap<>(); //look up table will help us to know which packet is from which file
		
		// These ArrayLists would play a role of packet holders
		ArrayList<byte[]> file1 = new ArrayList<>();
		ArrayList<byte[]> file2 = new ArrayList<>();
		ArrayList<byte[]> file3 = new ArrayList<>();

		// This ArrayList will contain other 3 ArrayLists (files) so then we can access them through index which will be stored in the look up table
		ArrayList<ArrayList<byte[]>> files = new ArrayList<>();
		files.add(file1);
		files.add(file2);
		files.add(file3);
		
		/*
		 * This indicator is used for two purposes: 
		 * 1) Indicates which file was not placed into lookUpTable
		 * 2) Used as a value for lookUpTable
		 */
		int indicator = 0;
		
		// String array which will have file names
		String[] fileNames = new String[3];

		boolean thisIsIt = false; // This boolean will be set to "true" once we know the total amount of packets that we are suppose to receive
		int fileCounter = 0; // Once its three we know that terminator now contains the total number of packets from three files
		int terminator = 0; // This terminator will represent the total number of packages of all 3 files
		int counter = 0; // Represent the number of packages that were received
		boolean keepReceiving = true; 
		byte fileID;

		//Keep getting answer
		while(keepReceiving){
			// Receive the packet
			byte[] buffer = new byte[1028];
			DatagramPacket packetToReceive = new DatagramPacket(buffer, buffer.length);
			socket.receive(packetToReceive);

			if((packetToReceive.getData()[0] & 1 )== 1){ //Check if the packet is not the "header"
				if (((packetToReceive.getData()[0] >> 1) & 1) == 1) { //check if the packet is the last one
					// last packet //
					fileID = packetToReceive.getData()[1]; 
					if(lookUpTable.containsKey(fileID)){
						int fileLocation = lookUpTable.get(fileID); // fileLocation will represent the index of the right file in the arrayList (files)
						int length = packetToReceive.getLength();
						byte[] data = new byte[length]; // Since the last packet might contain less that 1028 bytes we will trim the buffer to represent the actual size of received bytes
						data = Arrays.copyOfRange(packetToReceive.getData(), 0, length);
						files.get(fileLocation).add(data); // Adding this packet to the right arrayList (file)
						fileCounter++;
						counter++;

						int val = ((packetToReceive.getData()[2] & 0xff) << 8) | (packetToReceive.getData()[3] & 0xff);
						terminator += (val + 2); // Since this is the last packet we will add his packet number to the terminator plus 2 bytes since packet numbers are zero based and because of header file

					}else{
						lookUpTable.put(fileID,indicator);
						int fileLocation = indicator; // fileLocation will represent the index of the right file in the arrayList (files)
						int length = packetToReceive.getLength();
						byte[] data = new byte[length]; // Since the last packet might contain less that 1028 bytes we will trim the buffer to represent the actual size of received bytes
						data = Arrays.copyOfRange(packetToReceive.getData(), 0, length);
						files.get(fileLocation).add(data); // Adding this packet to the right arrayList (file)
						fileCounter++;
						counter++;
						indicator++;

						int val = ((packetToReceive.getData()[2] & 0xff) << 8) | (packetToReceive.getData()[3] & 0xff);
						terminator += (val + 2);  // Since this is the last packet we will add his packet number to the terminator plus 2 bytes since packet numbers are zero based and because of header file

					}

					if(fileCounter == 3 ){
						thisIsIt = true;
					}

				}else{
					// data packet //

					fileID = packetToReceive.getData()[1];
					if(lookUpTable.containsKey(fileID)){
						int fileLocation = lookUpTable.get(fileID);
						files.get(fileLocation).add(packetToReceive.getData());
						counter++;
					}else{
						lookUpTable.put(fileID,indicator);
						files.get(indicator).add(packetToReceive.getData());
						counter++; //Since we received the header packet we increment the number of packets that were received
						indicator++;
					}
				}
			}else{
				// header packet/ /

				fileID = packetToReceive.getData()[1];

				if(lookUpTable.containsKey(fileID)){
					byte[] fileNameData  = new byte[packetToReceive.getLength() - 2];
					fileNameData = Arrays.copyOfRange(packetToReceive.getData(), 2, packetToReceive.getLength());
					fileNames[lookUpTable.get(fileID)] = new String(fileNameData); //Store fileName into fileNames array
					counter++; //Since we received the header packet we increment the number of packets that were received
				}else{
					lookUpTable.put(fileID,indicator);
					byte[] fileNameData  = new byte[packetToReceive.getLength() - 2];
					fileNameData = Arrays.copyOfRange(packetToReceive.getData(), 2, packetToReceive.getLength());
					fileNames[indicator] = new String(fileNameData);
					counter++; //Since we received the header packet we increment the number of packets that were received
					indicator++;
				}
			}
			if(thisIsIt){
				if(counter == terminator) keepReceiving = false;
			}
		}
		
		socket.close();
		
		//Converting ArrayLists of byte arrays into array of byte arrays
		byte[][] fileArray1 = new byte[file1.size()][];
		byte[][] fileArray2 = new byte[file2.size()][];
		byte[][] fileArray3 = new byte[file3.size()][];

		for(int i = 0; i < fileArray1.length; i++){
			fileArray1[i] = file1.get(i);
		}

		for(int i = 0; i < fileArray2.length; i++){
			fileArray2[i] = file2.get(i);
		}

		for(int i = 0; i < fileArray3.length; i++){
			fileArray3[i] = file3.get(i);
		}

		//Sorting packets
		Arrays.sort(fileArray1, new FileSorter());
		Arrays.sort(fileArray2, new FileSorter());
		Arrays.sort(fileArray3, new FileSorter());

		
		// Write binary data to the file
		int indexForFile1 = lookUpTable.get(fileArray1[0][1]);
		FileOutputStream File1 = new FileOutputStream("./" + fileNames[indexForFile1]);
		for(int i = 0; i < fileArray1.length ; i++){
			File1.write(fileArray1[i], 4, fileArray1[i].length - 4);
		}

		int indexForFile2 = lookUpTable.get(fileArray2[0][1]);
		FileOutputStream File2 = new FileOutputStream("./" + fileNames[indexForFile2]);
		for(int i = 0; i < fileArray2.length ; i++){
			File2.write(fileArray2[i], 4, fileArray2[i].length - 4);
		}

		int indexForFile3 = lookUpTable.get(fileArray3[0][1]);
		FileOutputStream File3 = new FileOutputStream("./" + fileNames[indexForFile3]);
		for(int i = 0; i < fileArray3.length ; i++){
			File3.write(fileArray3[i], 4, fileArray3[i].length - 4);
		}

		File1.flush();
		File2.flush();
		File3.flush();

		File1.close();
		File2.close();
		File3.close();
	}

	
	// A simple comparator to sort array of bytes
	private static class FileSorter implements Comparator<byte[]>{

		@Override
		public int compare (byte[] b1, byte[] b2){
			// Getting packet numbers
			int num1 = ((b1[2] & 0xff) << 8) | (b1[3] & 0xff); 
			int num2 = ((b2[2] & 0xff) << 8) | (b2[3] & 0xff);
			int result = 0;
            if (num1 < num2) {
                    result = -1;
            } else if (num1 > num2) {
                    result = 1;
            }
			return result;
		}
	}

}
