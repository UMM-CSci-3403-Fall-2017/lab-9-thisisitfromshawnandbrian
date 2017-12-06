package segmentedfilesystem;

import static org.junit.Assert.*;

import org.junit.Test;

import segmentedfilesystem.FileTool.FileSorter;

/**
 * This is just a stub test file. You should rename it to
 * something meaningful in your context and populate it with
 * useful tests.
 */
public class UnitTests {

    @Test
    public void testComparator() {
        byte[] mockedArray = {1, 2, 3, 4, 5, 6, 7};
        FileSorter sorter  = new FileSorter();
        assertEquals((int)772, sorter.getPacketNumber(mockedArray));
        // Because if we take (mockedArray[2] << 8)|mockedArray[3] it will be 772
        assertEquals((int)((mockedArray[2] << 8)|mockedArray[3]), (int)772);
    }


    @Test
    public void testStatusChecker() {
       byte[] header = {2, 3, 4, 6}; //Since header[0] is 2 then its even. If first byte is even then its a header
       PacketReceiver receiver = new PacketReceiver();
       assertEquals(1, receiver.checkStatus(header)); // 1 represents header file

       byte[] usualDataPacket = {5, 3, 4, 6};// Since usualDataPacket[0] is 5 which is odd, this packet is just a data packet
       assertEquals(2, receiver.checkStatus(usualDataPacket)); // 2 represents usual data packet

       byte[] lastPacket = {3, 3, 4, 6};// Since lastPacket[1] is 3, it means its a last packet
       assertEquals(3, receiver.checkStatus(lastPacket)); // 3 represents last packet
    }

}
