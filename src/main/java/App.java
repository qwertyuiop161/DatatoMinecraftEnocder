import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Scanner;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class App {
    public static void main(String[] args) throws Exception {
        File folder = new File(".");
        File[] files = folder.listFiles();
        File inputFile = null;
        File mcaFile = null;

        for (File f : files) {
            if (f.getName().startsWith("input.") && f.isFile()) {
                inputFile = f;
            }

            if (f.getName().equals("r.0.0.mca")) {
                mcaFile = f;
            }
        }

        if (inputFile != null && mcaFile == null) {
            System.out.println("Input file detected: " + inputFile.getName());
            encodeFileToMCA(inputFile.getName(), "r.0.0.mca");
            return;
        }

        if (mcaFile != null && inputFile == null) {
            System.out.println("MCA detected. Reconstructing file...");

            String outputName = "reconstructed.bin";
            decodeMCA("r.0.0.mca", outputName);

            System.out.println("File restored as " + outputName);
            return;
        }

        System.out.println("Place ONE of the following in the folder:");
        System.out.println("  input.<anything>");
        System.out.println("  r.0.0.mca");
    }

    private static void encodeFileToMCA(String inputPath, String outputMCA) throws Exception {

        byte[] fileBytes = Files.readAllBytes(Paths.get(inputPath));
        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        byte[] nbtData = createNBT(base64);

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(compressed);
        deflater.write(nbtData);
        deflater.close();

        byte[] chunkData = compressed.toByteArray();

        ByteArrayOutputStream finalChunk = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(finalChunk);

        dos.writeInt(chunkData.length + 1);
        dos.writeByte(2);
        dos.write(chunkData);
        dos.close();

        byte[] fullChunk = finalChunk.toByteArray();
        RandomAccessFile raf = new RandomAccessFile(outputMCA, "rw");
        byte[] header = new byte[8192];
        raf.write(header);
        int sector = 2;
        raf.seek(sector * 4096);
        raf.write(fullChunk);

        int sectorsUsed = (fullChunk.length + 4095) / 4096;
        raf.seek(0);
        raf.write((sector >> 16) & 0xFF);
        raf.write((sector >> 8) & 0xFF);
        raf.write(sector & 0xFF);
        raf.write(sectorsUsed);

        raf.close();

        System.out.println("Encoded successfully.");
    }

    private static void decodeMCA(String mcaPath, String outputPath) throws Exception {

        RandomAccessFile raf = new RandomAccessFile(mcaPath, "r");

        raf.seek(0);
        int offset = (raf.readUnsignedByte() << 16)
                | (raf.readUnsignedByte() << 8)
                | raf.readUnsignedByte();
        int sectors = raf.readUnsignedByte();

        if (offset == 0) {
            throw new RuntimeException("No chunk found in (0,0)");
        }

        raf.seek(offset * 4096);

        int length = raf.readInt();
        int compression = raf.readUnsignedByte();

        byte[] compressed = new byte[length - 1];
        raf.readFully(compressed);
        raf.close();

        InflaterInputStream inflater = new InflaterInputStream(
                new ByteArrayInputStream(compressed));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int read;
        while ((read = inflater.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }

        byte[] nbtData = baos.toByteArray();

        String base64 = extractNBTString(nbtData);

        byte[] original = Base64.getDecoder().decode(base64);
        Files.write(Paths.get(outputPath), original);

        System.out.println("Decoded successfully.");
    }

    private static byte[] createNBT(String data) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(10);
        dos.writeShort(0);
        dos.writeByte(8);
        dos.writeShort(4);
        dos.writeBytes("data");
        dos.writeShort(data.length());
        dos.writeBytes(data);

        dos.writeByte(0);

        dos.close();
        return baos.toByteArray();
    }

    private static String extractNBTString(byte[] nbt) throws Exception {

        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(nbt));

        dis.readByte();
        dis.readShort();

        dis.readByte();
        dis.readShort();

        byte[] name = new byte[4];
        dis.readFully(name);

        int strLen = dis.readUnsignedShort();
        byte[] strBytes = new byte[strLen];
        dis.readFully(strBytes);

        return new String(strBytes);
    }
}