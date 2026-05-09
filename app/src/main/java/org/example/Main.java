package org.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import net.querz.nbt.tag.*;
import net.querz.nbt.io.*;

public class Main {
    static int cap;
    static int BASE;
    static String[] idMap;
    static Map<String, Integer> reverseMap = new HashMap<>();
    static Map<Integer, String> regularMap = new HashMap<>();

    static final int DATA_VERSION = 4671;
    static final int MIN_SECTION = -4;
    static final int MAX_SECTION = 19;
    static final int MIN_Y = -64;
    static final int MAX_Y = 319;

    static JLabel statusLabel;

    public static void loadJson() throws Exception {
        InputStream stream = Main.class.getResourceAsStream("/ids.json");
        if (stream == null)
            throw new FileNotFoundException("ids.json not found in classpath");
        JSONParser parser = new JSONParser();
        JSONArray a = (JSONArray) parser.parse(new InputStreamReader(stream));
        idMap = new String[a.size()];
        for (int i = 0; i < a.size(); i++) {
            idMap[i] = (String) a.get(i);
            reverseMap.put(idMap[i], i);
        }
        BASE = idMap.length;
        for (Map.Entry<String, Integer> entry : reverseMap.entrySet()) {
            regularMap.put(entry.getValue(), entry.getKey());
        }
        cap = 512 * 512 * 384;
    }

    public static void main(String[] args) throws Exception {
        loadJson();

        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame frame = new JFrame("Data Encryptor");
        frame.setLayout(new FlowLayout());

        JButton encButton = new JButton("Select File to Encode");
        JButton decButton = new JButton("Select MCA to Decode");
        statusLabel = new JLabel("Idle");
        statusLabel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel panel = new JPanel();
        encButton.setPreferredSize(new Dimension(200, 50));
        decButton.setPreferredSize(new Dimension(200, 50));
        statusLabel.setPreferredSize(new Dimension(75, 30));

        encButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    setStatus("Selecting file...");
                    Path selectedFile = getFile();
                    if (selectedFile == null) {
                        setStatus("Idle");
                        return;
                    }

                    setStatus("Encoding...");
                    List<String> blockNames = enc(selectedFile.toString());

                    setStatus("Writing MCA...");
                    Path out = downloadsDir().resolve("r.0.0.mca");
                    writeMCA(blockNames, out.toString());

                    Files.deleteIfExists(selectedFile);
                    setStatus("Done! Saved to Downloads/r.0.0.mca");

                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Error: " + ex.getMessage());
                }
            }).start();
        });

        decButton.addActionListener(e -> {
            new Thread(() -> {
                try {
                    setStatus("Selecting MCA...");
                    Path mcaFile = getFile();
                    if (mcaFile == null) {
                        setStatus("Idle");
                        return;
                    }

                    setStatus("Reading MCA...");
                    List<String> blockNames = readMCA(mcaFile.toString());

                    setStatus("Decoding...");
                    Path outFile = decToDownloads(blockNames);

                    Files.deleteIfExists(mcaFile);
                    setStatus("Done! Saved to Downloads/" + outFile.getFileName());

                } catch (Exception ex) {
                    ex.printStackTrace();
                    setStatus("Error: " + ex.getMessage());
                }
            }).start();
        });

        panel.add(encButton);
        panel.add(decButton);
        panel.add(statusLabel);
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(900, 150);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    static void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    static Path downloadsDir() {
        Path dl = Paths.get(System.getProperty("user.home"), "Downloads");
        if (!Files.exists(dl))
            dl = Paths.get(System.getProperty("user.home"));
        return dl;
    }

    public static List<String> readMCA(String mcaPath) throws IOException {
        List<String> blockNames = new ArrayList<>();
        int REGION_SIZE = 32;

        try (RandomAccessFile raf = new RandomAccessFile(mcaPath, "r")) {
            int[][] locations = new int[REGION_SIZE][REGION_SIZE];

            for (int z = 0; z < REGION_SIZE; z++) {
                for (int x = 0; x < REGION_SIZE; x++) {
                    int entry = raf.readInt();
                    locations[z][x] = (entry >> 8) & 0xFFFFFF;
                }
            }

            boolean done = false;

            for (int chunkZ = 0; chunkZ < REGION_SIZE && !done; chunkZ++) {
                for (int chunkX = 0; chunkX < REGION_SIZE && !done; chunkX++) {
                    int offset = locations[chunkZ][chunkX];
                    if (offset == 0)
                        continue;

                    raf.seek((long) offset * 4096);
                    int dataLen = raf.readInt();
                    int compression = raf.readByte() & 0xFF;
                    byte[] compressed = new byte[dataLen - 1];
                    raf.readFully(compressed);

                    byte[] nbtBytes = decompress(compressed, compression);

                    NBTDeserializer deserializer = new NBTDeserializer(false);
                    NamedTag namedTag = deserializer.fromStream(
                            new ByteArrayInputStream(nbtBytes));
                    CompoundTag root = (CompoundTag) namedTag.getTag();

                    ListTag<?> sections = (ListTag<?>) root.get("sections");
                    if (sections == null)
                        continue;

                    Map<Integer, String[]> sectionMap = new HashMap<>();
                    for (int si = 0; si < sections.size(); si++) {
                        CompoundTag section = (CompoundTag) sections.get(si);
                        int sectionY = section.getByte("Y");
                        CompoundTag blockStates = (CompoundTag) section.get("block_states");
                        if (blockStates == null)
                            continue;

                        ListTag<?> paletteTag = (ListTag<?>) blockStates.get("palette");
                        if (paletteTag == null)
                            continue;

                        List<String> palette = new ArrayList<>();
                        for (int pi = 0; pi < paletteTag.size(); pi++) {
                            CompoundTag entry = (CompoundTag) paletteTag.get(pi);
                            palette.add(entry.getString("Name"));
                        }

                        String[] flat = new String[4096];
                        if (palette.size() == 1) {
                            Arrays.fill(flat, palette.get(0));
                        } else {
                            long[] data = blockStates.getLongArray("data");
                            int bitsPerEntry = Math.max(4,
                                    Integer.SIZE - Integer.numberOfLeadingZeros(
                                            palette.size() - 1));
                            int indicesPerLong = 64 / bitsPerEntry;
                            long mask = (1L << bitsPerEntry) - 1;
                            for (int i = 0; i < 4096; i++) {
                                int longIdx = i / indicesPerLong;
                                int bitOffset = (i % indicesPerLong) * bitsPerEntry;
                                int paletteIdx = (int) ((data[longIdx] >> bitOffset) & mask);
                                flat[i] = palette.get(paletteIdx);
                            }
                        }
                        sectionMap.put(sectionY, flat);
                    }

                    outer: for (int y = MAX_Y; y >= MIN_Y; y--) {
                        int sectionY = y >> 4;
                        int localY = y & 0xF;
                        String[] flat = sectionMap.get(sectionY);
                        
                        for (int bx = 0; bx < 16; bx++) {
                            for (int bz = 0; bz < 16; bz++) {
                                String name;
                                if (flat == null) {
                                    name = "minecraft:air";
                                } else {
                                    int blockIdx = localY * 256 + bx * 16 + bz;
                                    name = flat[blockIdx];
                                }

                                if (name.equals(idMap[0])) {
                                    done = true;
                                    break outer;
                                }
                                blockNames.add(name);
                            }
                        }
                    }
                }
            }
        }
        return blockNames;
    }

    private static byte[] decompress(byte[] data, int compression)
            throws IOException {
        InputStream in;
        if (compression == 2) {
            in = new java.util.zip.InflaterInputStream(
                    new ByteArrayInputStream(data));
        } else if (compression == 1) {
            in = new java.util.zip.GZIPInputStream(
                    new ByteArrayInputStream(data));
        } else {
            return data;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1)
            out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    public static Path decToDownloads(List<String> blockNames) throws IOException {
        List<Integer> l = new ArrayList<>();
        for (String name : blockNames) {
            Integer val = reverseMap.get(name);
            if (val == null) {
                System.err.println("unknown token skipped: [" + name + "]");
            } else {
                l.add(val - 1); // undo the +1 shift
            }
        }

        int idx = 0;
        int el = l.get(idx++);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < el; i++)
            sb.append((char) (int) l.get(idx++));
        String ex = sb.toString();

        long target = (long) l.get(idx++) * (BASE - 1) * (BASE - 1)
                + (long) l.get(idx++) * (BASE - 1)
                + (long) l.get(idx++);

        java.math.BigInteger BIG_BASE = java.math.BigInteger.valueOf(BASE - 1);
        java.math.BigInteger num = java.math.BigInteger.ZERO;
        for (int i = idx; i < l.size(); i++)
            num = num.multiply(BIG_BASE)
                    .add(java.math.BigInteger.valueOf(l.get(i)));

        byte[] r = new byte[(int) target];
        for (int i = (int) target - 1; i >= 0; i--) {
            java.math.BigInteger[] divRem = num.divideAndRemainder(java.math.BigInteger.valueOf(256));
            r[i] = divRem[1].byteValue();
            num = divRem[0];
        }

        Path out = downloadsDir().resolve("output." + ex);
        Files.write(out, r);
        return out;
    }

    public static void writeMCA(List<String> blockNames, String outputPath)
            throws IOException {
        int REGION_SIZE = 32;
        byte[][] chunkData = new byte[REGION_SIZE * REGION_SIZE][];
        int listIdx = 0;

        for (int chunkZ = 0; chunkZ < REGION_SIZE; chunkZ++) {
            for (int chunkX = 0; chunkX < REGION_SIZE; chunkX++) {
                int numSections = MAX_SECTION - MIN_SECTION + 1;
                String[][] sectionBlocks = new String[numSections][4096];
                for (String[] sec : sectionBlocks)
                    Arrays.fill(sec, "minecraft:air");

                boolean done = false;
                outer: for (int y = MAX_Y; y >= MIN_Y; y--) {
                    int sectionIdx = (y >> 4) - MIN_SECTION;
                    int localY = y & 0xF;
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            if (listIdx >= blockNames.size()) {
                                done = true;
                                break outer;
                            }
                            String name = blockNames.get(listIdx++);
                            int blockIdx = localY * 256 + bx * 16 + bz;
                            sectionBlocks[sectionIdx][blockIdx] = name;
                        }
                    }
                }

                CompoundTag root = buildChunkNBT(
                        chunkX, chunkZ, sectionBlocks, numSections);
                byte[] nbtBytes = serializeNBT(root);
                byte[] chunkEntry = new byte[5 + nbtBytes.length];
                int dataLen = nbtBytes.length + 1;
                chunkEntry[0] = (byte) (dataLen >> 24);
                chunkEntry[1] = (byte) (dataLen >> 16);
                chunkEntry[2] = (byte) (dataLen >> 8);
                chunkEntry[3] = (byte) (dataLen);
                chunkEntry[4] = 2;
                System.arraycopy(nbtBytes, 0, chunkEntry, 5, nbtBytes.length);
                chunkData[chunkZ * REGION_SIZE + chunkX] = chunkEntry;

                if (done) {
                    for (int i = chunkZ * REGION_SIZE + chunkX + 1; i < REGION_SIZE * REGION_SIZE; i++) {
                        chunkData[i] = buildEmptyChunkBytes(
                                i % REGION_SIZE, i / REGION_SIZE);
                    }
                    break;
                }
            }
            if (listIdx >= blockNames.size())
                break;
        }

        for (int i = 0; i < chunkData.length; i++) {
            if (chunkData[i] == null)
                chunkData[i] = buildEmptyChunkBytes(
                        i % REGION_SIZE, i / REGION_SIZE);
        }

        int[] offsets = new int[REGION_SIZE * REGION_SIZE];
        int[] sizes = new int[REGION_SIZE * REGION_SIZE];
        int currentSector = 2;
        for (int i = 0; i < chunkData.length; i++) {
            int paddedLen = ((chunkData[i].length + 4095) / 4096) * 4096;
            offsets[i] = currentSector;
            sizes[i] = paddedLen / 4096;
            currentSector += paddedLen / 4096;
        }

        try (RandomAccessFile raf = new RandomAccessFile(outputPath, "rw")) {
            raf.setLength(0);
            for (int i = 0; i < REGION_SIZE * REGION_SIZE; i++)
                raf.writeInt((offsets[i] << 8) | (sizes[i] & 0xFF));
            int timestamp = (int) (System.currentTimeMillis() / 1000L);
            for (int i = 0; i < REGION_SIZE * REGION_SIZE; i++)
                raf.writeInt(timestamp);
            for (byte[] chunk : chunkData) {
                raf.write(chunk);
                int pad = (4096 - (chunk.length % 4096)) % 4096;
                if (pad > 0)
                    raf.write(new byte[pad]);
            }
        }
    }

    private static byte[] buildEmptyChunkBytes(int chunkX, int chunkZ)
            throws IOException {
        int numSections = MAX_SECTION - MIN_SECTION + 1;
        String[][] sectionBlocks = new String[numSections][4096];
        for (String[] sec : sectionBlocks)
            Arrays.fill(sec, "minecraft:air");
        CompoundTag root = buildChunkNBT(chunkX, chunkZ, sectionBlocks, numSections);
        byte[] nbtBytes = serializeNBT(root);
        byte[] entry = new byte[5 + nbtBytes.length];
        int dataLen = nbtBytes.length + 1;
        entry[0] = (byte) (dataLen >> 24);
        entry[1] = (byte) (dataLen >> 16);
        entry[2] = (byte) (dataLen >> 8);
        entry[3] = (byte) (dataLen);
        entry[4] = 2;
        System.arraycopy(nbtBytes, 0, entry, 5, nbtBytes.length);
        return entry;
    }

    private static CompoundTag buildChunkNBT(
            int chunkX, int chunkZ,
            String[][] sectionBlocks, int numSections) {
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", DATA_VERSION);
        root.putInt("xPos", chunkX);
        root.putInt("zPos", chunkZ);
        root.putInt("yPos", MIN_SECTION);
        root.putString("Status", "minecraft:full");
        root.putLong("LastUpdate", 0L);
        root.putLong("InhabitedTime", 0L);

        ListTag<CompoundTag> sections = new ListTag<>(CompoundTag.class);
        for (int s = 0; s < numSections; s++) {
            int sectionY = s + MIN_SECTION;
            CompoundTag section = new CompoundTag();
            section.putByte("Y", (byte) sectionY);

            String[] flat = sectionBlocks[s];
            List<String> palette = new ArrayList<>();
            Map<String, Integer> paletteIndex = new LinkedHashMap<>();
            for (String name : flat) {
                if (!paletteIndex.containsKey(name)) {
                    paletteIndex.put(name, palette.size());
                    palette.add(name);
                }
            }

            CompoundTag blockStates = new CompoundTag();
            ListTag<CompoundTag> paletteTag = new ListTag<>(CompoundTag.class);
            for (String name : palette) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Name", name);
                paletteTag.add(entry);
            }
            blockStates.put("palette", paletteTag);

            if (palette.size() > 1) {
                int bitsPerEntry = Math.max(4,
                        Integer.SIZE - Integer.numberOfLeadingZeros(palette.size() - 1));
                long[] data = packBlockStates(flat, paletteIndex, bitsPerEntry);
                blockStates.putLongArray("data", data);
            }

            section.put("block_states", blockStates);

            CompoundTag biomes = new CompoundTag();
            ListTag<StringTag> biomePalette = new ListTag<>(StringTag.class);
            biomePalette.add(new StringTag("minecraft:plains"));
            biomes.put("palette", biomePalette);
            section.put("biomes", biomes);

            sections.add(section);
        }
        root.put("sections", sections);
        root.put("block_entities", new ListTag<>(CompoundTag.class));
        root.put("Heightmaps", new CompoundTag());
        return root;
    }

    private static long[] packBlockStates(
            String[] flat,
            Map<String, Integer> paletteIndex,
            int bitsPerEntry) {
        int indicesPerLong = 64 / bitsPerEntry;
        int arrayLen = (int) Math.ceil(4096.0 / indicesPerLong);
        long[] data = new long[arrayLen];
        for (int i = 0; i < 4096; i++) {
            int idx = paletteIndex.get(flat[i]);
            int longIndex = i / indicesPerLong;
            int bitOffset = (i % indicesPerLong) * bitsPerEntry;
            data[longIndex] |= ((long) idx) << bitOffset;
        }
        return data;
    }

    private static byte[] serializeNBT(CompoundTag tag) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        new NBTSerializer(false).toStream(new NamedTag("", tag), raw);
        byte[] uncompressed = raw.toByteArray();

        java.util.zip.Deflater deflater = new java.util.zip.Deflater();
        deflater.setInput(uncompressed);
        deflater.finish();
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!deflater.finished()) {
            int count = deflater.deflate(buf);
            compressed.write(buf, 0, count);
        }
        deflater.end();
        return compressed.toByteArray();
    }

    public static Path getFile() throws Exception {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File externalFile = fileChooser.getSelectedFile();
            Path destination = Paths.get(externalFile.getName());
            Files.copy(externalFile.toPath(), destination,
                    StandardCopyOption.REPLACE_EXISTING);
            return destination;
        }
        return null;
    }

    public static List<String> enc(String f) throws IOException {
        byte[] by = Files.readAllBytes(Paths.get(f));
        String filenameOnly = Paths.get(f).getFileName().toString();
        String ex = filenameOnly.substring(filenameOnly.lastIndexOf(".") + 1);
        List<Integer> l = new ArrayList<>();
        l.add(ex.length());
        for (char ch : ex.toCharArray())
            l.add((int) ch);
        long len = by.length;
        l.add((int) ((len / ((long) (BASE - 1) * (BASE - 1))) % (BASE - 1)));
        l.add((int) ((len / (BASE - 1)) % (BASE - 1)));
        l.add((int) (len % (BASE - 1)));

        java.math.BigInteger BIG_BASE = java.math.BigInteger.valueOf(BASE - 1);
        java.math.BigInteger num = java.math.BigInteger.ZERO;
        for (byte v : by) {
            num = num.shiftLeft(8).or(java.math.BigInteger.valueOf(v & 0xFF));
        }
        List<Integer> dataDigits = new ArrayList<>();
        if (num.equals(java.math.BigInteger.ZERO)) {
            dataDigits.add(0);
        } else {
            while (num.compareTo(java.math.BigInteger.ZERO) > 0) {
                java.math.BigInteger[] divRem = num.divideAndRemainder(BIG_BASE);
                dataDigits.add(divRem[1].intValue());
                num = divRem[0];
            }
        }
        Collections.reverse(dataDigits);
        l.addAll(dataDigits);

        // Shift all values up by 1 so 0 (air) is never used as a data value
        List<String> stringList = new ArrayList<>();
        for (int val : l) {
            stringList.add(idMap[val + 1]);
        }
        // Pad with air (index 0) as end-of-data sentinel
        while (stringList.size() < cap)
            stringList.add(idMap[0]);
        return stringList;
    }
}