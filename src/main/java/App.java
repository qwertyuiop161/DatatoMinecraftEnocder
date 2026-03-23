import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.zip.*;

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

            System.out.println("Encoding file: " + inputFile.getName());
            encodeFileToMCA(inputFile);
            System.out.println("Created r.0.0.mca");
            return;

        }

        if (mcaFile != null && inputFile == null) {

            System.out.println("Decoding r.0.0.mca...");
            decodeMCA("r.0.0.mca");
            return;

        }

        System.out.println("Place ONE of the following in the folder:");
        System.out.println("input.<anything>");
        System.out.println("or");
        System.out.println("r.0.0.mca");
    }

    private static void encodeFileToMCA(File inputFile) throws Exception {

        byte[] fileBytes = Files.readAllBytes(inputFile.toPath());

        String base64 = Base64.getEncoder().encodeToString(fileBytes);

        byte[] nbtData = createNBT(inputFile.getName(), base64);

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

        RandomAccessFile raf = new RandomAccessFile("r.0.0.mca", "rw");

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
    }

    private static void decodeMCA(String mcaPath) throws Exception {

        RandomAccessFile raf = new RandomAccessFile(mcaPath, "r");

        raf.seek(0);

        int offset =
                (raf.readUnsignedByte() << 16)
                        | (raf.readUnsignedByte() << 8)
                        | raf.readUnsignedByte();

        raf.readUnsignedByte();

        if (offset == 0) {
            throw new RuntimeException("No chunk found");
        }

        raf.seek(offset * 4096);

        int length = raf.readInt();
        raf.readUnsignedByte();

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

        String[] extracted = extractNBT(nbtData);

        String filename = extracted[0];
        String base64 = extracted[1];

        byte[] original = Base64.getDecoder().decode(base64);

        Files.write(Paths.get(filename), original);

        System.out.println("Restored file: " + filename);
    }

    private static byte[] createNBT(String filename, String data) throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeByte(10);
        dos.writeShort(0);

        dos.writeByte(8);
        dos.writeShort(4);
        dos.writeBytes("name");
        dos.writeShort(filename.length());
        dos.writeBytes(filename);

        dos.writeByte(8);
        dos.writeShort(4);
        dos.writeBytes("data");
        dos.writeShort(data.length());
        dos.writeBytes(data);

        dos.writeByte(0);

        dos.close();
        return baos.toByteArray();
    }


    private static String[] extractNBT(byte[] nbt) throws Exception {

        DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(nbt));

        dis.readByte();
        dis.readShort();

        dis.readByte();
        dis.readShort();

        byte[] nameTag = new byte[4];
        dis.readFully(nameTag);

        int nameLen = dis.readUnsignedShort();
        byte[] nameBytes = new byte[nameLen];
        dis.readFully(nameBytes);

        String filename = new String(nameBytes);

        dis.readByte();
        dis.readShort();

        byte[] dataTag = new byte[4];
        dis.readFully(dataTag);

        int dataLen = dis.readUnsignedShort();
        byte[] dataBytes = new byte[dataLen];
        dis.readFully(dataBytes);

        String data = new String(dataBytes);

        return new String[]{filename, data};
    }
}