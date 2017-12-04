package segmentedfilesystem;

import java.io.File;
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
		HashMap<Byte, Integer> lookUpTable = new HashMap<>();
		ArrayList<byte[]> file1 = new ArrayList<>();
		ArrayList<byte[]> file2 = new ArrayList<>();
		ArrayList<byte[]> file3 = new ArrayList<>();

		ArrayList<ArrayList<byte[]>> files = new ArrayList<>();
		files.add(file1);
		files.add(file2);
		files.add(file3);

		int indicator = 0;

		String[] fileNames = new String[3];

		boolean thisIsIt = false; //This boolean will be set to "true" once we know the total amount of packets that we are suppose to receive
		int fileCounter = 0; //This counter will tell that we know how many packets should be received in total from 3 files
		int terminator = 0;
		int counter = 0;
		boolean keepReceiving = true;
		byte fileID;

		//Keep getting answer
		while(keepReceiving){
			byte[] buffer = new byte[1028];
			DatagramPacket packetToReceive = new DatagramPacket(buffer, buffer.length);
			socket.receive(packetToReceive);

			if((packetToReceive.getData()[0] & 1 )== 1){
				if (((packetToReceive.getData()[0] >> 1) & 1) == 1) { //check if the packet is the last one
					// last packet //
					fileID = packetToReceive.getData()[1];
					if(lookUpTable.containsKey(fileID)){
						int fileLocation = lookUpTable.get(fileID);
						int length = packetToReceive.getLength();
						byte[] data = new byte[length];
						data = Arrays.copyOfRange(packetToReceive.getData(), 0, length);
						files.get(fileLocation).add(data);
						fileCounter++;
						counter++;

						int val = ((packetToReceive.getData()[2] & 0xff) << 8) | (packetToReceive.getData()[3] & 0xff);
						terminator += (val + 2);

					}else{
						lookUpTable.put(fileID,indicator);
						int fileLocation = indicator;
						int length = packetToReceive.getLength();
						byte[] data = new byte[length];
						data = Arrays.copyOfRange(packetToReceive.getData(), 0, length);
						files.get(fileLocation).add(data);
						fileCounter++;
						counter++;
						indicator++;

						int val = ((packetToReceive.getData()[2] & 0xff) << 8) | (packetToReceive.getData()[3] & 0xff);
						terminator += (val + 2);

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
					fileNames[lookUpTable.get(fileID)] = new String(fileNameData);
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

		//Sorting packets

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

		Arrays.sort(fileArray1, new FileSorter());
		Arrays.sort(fileArray2, new FileSorter());
		Arrays.sort(fileArray3, new FileSorter());

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

		System.out.println(fileNames[0]);
		System.out.println(fileNames[1]);
		System.out.println(fileNames[2]);

	}

	private static class FileSorter implements Comparator<byte[]>{

		@Override
		public int compare (byte[] b1, byte[] b2){
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
