package io.socket.jeromq.utils;

/**
 * @author xuejian.sun
 * @date 2019-07-23 14:42
 */
public final class ByteUtil {

    private ByteUtil(){}

    public static byte[] merge(byte[] data1, byte[] data2){
        byte[] data = new byte[data1.length+data2.length];
        System.arraycopy(data1,0,data,0,data1.length);
        System.arraycopy(data2,0,data,data1.length,data2.length);
        return data;
    }

    public static byte[] subBytes(byte[] data, int offset){
        byte[] result = new byte[data.length - offset];
        System.arraycopy(data,offset,result,0,result.length);
        return result;
    }
}
