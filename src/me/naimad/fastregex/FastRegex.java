package me.naimad.fastregex;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class FastRegex {

    static {
        try {
            System.out.println("[FastRegex] Starting native library loading...");
            String os = System.getProperty("os.name").toLowerCase();
            String prefix = "native/";
            String filename;

            if (os.contains("win")) {
                prefix += "windows-x86_64/";
                filename = "fastregex.dll";
            } else if (os.contains("mac")) {
                prefix += "macos-aarch64/";
                filename = "libfastregex.dylib";
            } else {
                prefix += "linux-x86_64/";
                filename = "libfastregex.so";
            }

            String resourcePath = "/" + prefix + filename;
            java.net.URL resource = FastRegex.class.getResource(resourcePath);
            System.out.println("[FastRegex] Searching for resource (absolute): " + resourcePath);
            
            if (resource == null) {
                resourcePath = prefix + filename;
                resource = FastRegex.class.getClassLoader().getResource(resourcePath);
                System.out.println("[FastRegex] Searching for resource (relative): " + resourcePath);
            }

            if (resource == null) {
                // Try Thread context class loader
                resourcePath = prefix + filename;
                resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
                System.out.println("[FastRegex] Searching for resource (TCCL): " + resourcePath);
            }
            
            if (resource == null) {
                System.out.println("[FastRegex] Resource not found in JAR. Falling back to system library search.");
                System.loadLibrary("fastregex");
            } else {
                java.nio.file.Path temp = java.nio.file.Files.createTempFile("fastregex-", "-" + filename);
                System.out.println("[FastRegex] Extracting resource to: " + temp.toAbsolutePath());
                try (java.io.InputStream is = resource.openStream()) {
                    java.nio.file.Files.copy(is, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                System.load(temp.toAbsolutePath().toString());
                System.out.println("[FastRegex] Library loaded successfully.");
                temp.toFile().deleteOnExit();
            }
        } catch (Exception e) {
            System.err.println("[FastRegex] Failed to load native library: " + e.getMessage());
            e.printStackTrace();
            try { System.loadLibrary("fastregex"); } catch (UnsatisfiedLinkError le) {}
        }
    }

    public static native long compile(String pattern);
    public static native void release(long handle);
    public static native boolean matchesUtf8Direct(long handle, ByteBuffer directBuf, int offset, int len);
    public static native void batchMatchesUtf8Direct(long handle, ByteBuffer dataBuf, int[] offsets, int[] lengths, long[] outBits);

    public static class PackedUtf8 {
        public ByteBuffer data;
        public int[] offsets;
        public int[] lengths;
    }

    public static PackedUtf8 packUtf8Direct(String[] batch) {
        int totalLen = 0;
        byte[][] bytesArray = new byte[batch.length][];
        for (int i = 0; i < batch.length; i++) {
            bytesArray[i] = batch[i].getBytes(StandardCharsets.UTF_8);
            totalLen += bytesArray[i].length;
        }

        ByteBuffer data = ByteBuffer.allocateDirect(totalLen);
        int[] offsets = new int[batch.length];
        int[] lengths = new int[batch.length];
        int currentPos = 0;
        for (int i = 0; i < batch.length; i++) {
            offsets[i] = currentPos;
            lengths[i] = bytesArray[i].length;
            data.put(bytesArray[i]);
            currentPos += bytesArray[i].length;
        }

        PackedUtf8 res = new PackedUtf8();
        res.data = data;
        res.offsets = offsets;
        res.lengths = lengths;
        return res;
    }

    public static boolean getBit(long[] outBits, int i) {
        int wordIdx = i / 64;
        int bitIdx = i % 64;
        return (outBits[wordIdx] & (1L << bitIdx)) != 0;
    }
}