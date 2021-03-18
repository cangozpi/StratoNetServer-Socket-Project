/* This class embodies the TCP Payload structure*/

import java.nio.ByteBuffer;

public class TCPPayload {

    //global variables
    private int phase; //0 -> initialization | 1 -> query
    private int type; // 1 -> Auth_Challenge | 2 -> Auth_Fail | 3 -> Auth_Success
    private int size; // size of the payload in bytes
    private int dsSize;//size of the payload sent over the dataSocket
    private String payload; // String of size of n bytes

    public TCPPayload(int phase, int type, int size, String payload) {
        this.phase = phase;
        this.type = type;
        this.size = size;
        this.payload = payload;
    }

    //overloaded constructor for API StratoNet Protocol (phase 1)
    public TCPPayload(int phase, int type, int size, String payload, int dsSize) {
        this.phase = phase;
        this.type = type;
        this.size = size;
        this.payload = payload;
        this.dsSize = dsSize;
    }

    //convert object to StratoNet protocol String message(used for phase 0)
    public byte[] toStratonetProtocolByteArray(){
        //convert info to bytes
        byte[] phaseByteArray = ByteBuffer.allocate(1).put((byte)phase).array();
        byte[] typeByteArray = ByteBuffer.allocate(1).put((byte)type).array();
        byte[] payloadByteArray = payload.getBytes();
        byte[] sizeByteArray = ByteBuffer.allocate(4).putInt(payloadByteArray.length).array();//byte size of payload

        //concatenate byte arrays to one packet
        byte[] packet = new byte[phaseByteArray.length + typeByteArray.length + sizeByteArray.length + payloadByteArray.length];
        System.arraycopy(phaseByteArray, 0, packet, 0, phaseByteArray.length);
        System.arraycopy(typeByteArray, 0, packet, phaseByteArray.length, typeByteArray.length);
        System.arraycopy(sizeByteArray, 0, packet, phaseByteArray.length + typeByteArray.length, sizeByteArray.length);
        System.arraycopy(payloadByteArray, 0, packet, phaseByteArray.length + typeByteArray.length + sizeByteArray.length, payloadByteArray.length);

        return packet;
    }


    //convert object to API StratoNet protocol String message(used for phase 1)
    public byte[] toAPIStratonetProtocolByteArray(){
        //convert info to bytes
        byte[] phaseByteArray = ByteBuffer.allocate(1).put((byte)phase).array();
        byte[] typeByteArray = ByteBuffer.allocate(1).put((byte)type).array();
        byte[] payloadByteArray = payload.getBytes();
        byte[] sizeByteArray = ByteBuffer.allocate(4).putInt(payloadByteArray.length).array();//byte size of payload
        byte[] dsSizeByteArray = ByteBuffer.allocate(4).putInt(dsSize).array();//byte size of payload

        //concatenate byte arrays to one packet
        byte[] packet = new byte[phaseByteArray.length + typeByteArray.length + sizeByteArray.length + payloadByteArray.length + dsSizeByteArray.length];
        System.arraycopy(phaseByteArray, 0, packet, 0, phaseByteArray.length);
        System.arraycopy(typeByteArray, 0, packet, phaseByteArray.length, typeByteArray.length);
        System.arraycopy(sizeByteArray, 0, packet, phaseByteArray.length + typeByteArray.length, sizeByteArray.length);
        System.arraycopy(dsSizeByteArray, 0, packet, phaseByteArray.length + typeByteArray.length + sizeByteArray.length,  dsSizeByteArray.length);

        System.arraycopy(payloadByteArray, 0, packet, phaseByteArray.length + typeByteArray.length + sizeByteArray.length + dsSizeByteArray.length, payloadByteArray.length);

        return packet;
    }


    //getters and setters

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

}
