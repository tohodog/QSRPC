package org.song.qsrpc.zip;

import org.xerial.snappy.Snappy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by QSong
 * Contact github.com/tohodog
 * Date 2020/11/10
 * todo 可优化成 zero copy
 */
public class Zip {

    private static List<IZip> list = new ArrayList<>();

    static {

        IZip snappy = new IZip() {
            @Override
            public byte[] compress(byte[] bytes) throws IOException {
                bytes = Snappy.compress(bytes);
//                System.out.println("压缩" + bytes.length);
                return bytes;
            }

//            @Override
//            public byte[] compress(byte[] bytes, ByteBuf byteBuf) throws IOException {
//                 return new byte[0];
//            }

            @Override
            public byte[] uncompress(byte[] bytes) throws IOException {
                bytes = Snappy.uncompress(bytes);
//                System.out.println("解压" + bytes.length);
                return bytes;
            }
        };

        IZip gzip = new IZip() {
            @Override
            public byte[] compress(byte[] bytes) throws IOException {
//                long l = System.currentTimeMillis();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(out);
                gzip.write(bytes);
                gzip.flush();
                gzip.close();
                bytes = out.toByteArray();
//                System.out.println("压缩+" + bytes.length + " t:" + (System.currentTimeMillis() - l));
                return bytes;
            }

            @Override
            public byte[] uncompress(byte[] bytes) throws IOException {
//                long l = System.currentTimeMillis();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                GZIPInputStream ungzip = new GZIPInputStream(in);
                byte[] buffer = new byte[1024];
                int n;
                while ((n = ungzip.read(buffer)) >= 0) {
                    out.write(buffer, 0, n);
                }
                bytes = out.toByteArray();
                ungzip.close();//必须要写,不然内存卡死
//                System.out.println("解压+" + bytes.length + " t:" + (System.currentTimeMillis() - l));
                return bytes;
            }
        };
        list.add(snappy);
        list.add(gzip);
    }

    public static IZip get(String type) {
        return get(getInt(type));
    }

    public static IZip get(int type) {
        type--;
        if (type < 0 || type >= list.size()) return null;
        return list.get(type);
    }


    public static byte getInt(String type) {
        if (type == null || type.isEmpty()) return 0;
        switch (type.toLowerCase()) {
            case "snappy":
                return 1;
            case "gzip":
                return 2;
            default:
                return 0;
        }
    }
}
