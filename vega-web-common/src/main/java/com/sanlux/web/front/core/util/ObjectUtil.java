package com.sanlux.web.front.core.util;

import java.io.*;

/**
 * java序列化工具类
 * Created by lujm on 2017/4/9.
 */
public class ObjectUtil {
    /**
     * 对象转byte[]
     * @param obj 对象
     * @return byte[]
     * @throws IOException
     */
    public static byte[] object2Bytes(Object obj) throws IOException {
        ByteArrayOutputStream bo=new ByteArrayOutputStream();
        ObjectOutputStream oo=new ObjectOutputStream(bo);
        oo.writeObject(obj);
        byte[] bytes=bo.toByteArray();
        bo.close();
        oo.close();
        return bytes;
    }
    /**
     * byte[]转对象
     * @param bytes bytes
     * @return Object
     * @throws Exception
     */
    public static Object bytes2Object(byte[] bytes) throws Exception{
        ByteArrayInputStream in=new ByteArrayInputStream(bytes);
        ObjectInputStream sIn=new ObjectInputStream(in);
        return sIn.readObject();
    }
}
