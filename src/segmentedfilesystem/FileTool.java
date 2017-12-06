package segmentedfilesystem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FileTool {
	
	// FileTool is built on top of the ArrayList which will store packets in the order in which they were received
	ArrayList<byte[]> file = new ArrayList<>();
	
	// this field will contain the name of the file
	String fileName = "";
	
	/**
	 * This method just inserts byte array into ArrayList
	 */
	public void insert(byte[] data){
		file.add(data);
	}
	
	/**
	 * This method sets the filenName field
	 */
	public void setFileName(String fileName){
		this.fileName = fileName;
	}
	
	/**
	 * This method handles packets in the ArrayList
	 * Converts ArrayList into Array
	 * Sorts Array according to the packet number
	 */
	public void handlePackets() throws IOException{
		
		byte[][] fileArray = new byte[file.size()][];
		for(int i = 0; i < fileArray.length; i++){
			fileArray[i] = file.get(i);
		}
		
		Arrays.sort(fileArray, new FileSorter());
		
		FileOutputStream File = new FileOutputStream("./" + fileName);
		
		for(int i = 0; i < fileArray.length ; i++){
			File.write(fileArray[i], 4, fileArray[i].length - 4);
		}
		
		File.flush();
		File.close();
	}
	
	/*
	 * A simple comparator to sort array of bytes
	 */
	public static class FileSorter implements Comparator<byte[]>{
		@Override
		public int compare (byte[] b1, byte[] b2){
			int num1 = getPacketNumber(b1);
			int num2 = getPacketNumber(b2);;
			int result = 0;
			if (num1 < num2) {
				result = -1;
			} else if (num1 > num2) {
				result = 1;
			}
			return result;
		}
		
		/**
		 * This method extracts the packet number from packet
		 */
		public int getPacketNumber(byte[] data){
			int num = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
			return num;
		}
	}
}
