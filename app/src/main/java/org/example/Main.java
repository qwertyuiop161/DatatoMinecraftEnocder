package org.example;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.sql.DataSource;
import javax.xml.crypto.Data;

import org.jglrxavpok.hephaistos.data.RandomAccessFileSource;
import org.jglrxavpok.hephaistos.mca.BlockState;
import org.jglrxavpok.hephaistos.mca.ChunkColumn;
import org.jglrxavpok.hephaistos.mca.RegionFile;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class Main {
    static int cap = 512 * 512 * 384;
    static int BASE;
    static String[] idMap;
    static Map<String, Integer> reverseMap = new HashMap<>();

    public static void loadJson() throws Exception {
        JSONParser parser = new JSONParser();
        JSONArray a = (JSONArray) parser.parse(new FileReader("app/src/main/resources/ids.json"));
        idMap = new String[a.size()];
        for (int i = 0; i < a.size(); i++) {
            idMap[i] = (String) a.get(i);
            reverseMap.put(idMap[i], i);
        }
        BASE = idMap.length;
    }

    public static void fillChunk(ChunkColumn c, BlockState b) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    c.setBlockState(x, y, z, b);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        loadJson();
        Scanner s = new Scanner(System.in);
        // input for enc or dec, 1 is enc
        // store it in c
        // if(c==1){
        // System.out.println("file:");
        // String f = "app/src/main/resources/" + s.next();
        // List<String> r = enc(f);

        RandomAccessFile raf = new RandomAccessFile("r.0.0.mca", "rw");
        RandomAccessFileSource dataSource = new RandomAccessFileSource(raf);
        RegionFile region = new RegionFile(dataSource, 0, 0);
        for (int i = 0; i<16; i++) {
            ChunkColumn chunk = region.getOrCreateChunk(0, i);
            fillChunk(chunk, new BlockState("minecraft:emerald_block"));
            region.writeColumn(chunk);
        }
        // }else {
        // System.out.println("list:");
        // s.nextLine();
        // String in = s.nextLine();
        // String v = in.trim();
        // if (v.startsWith("["))
        // v = v.substring(1);
        // if (v.endsWith("]"))
        // v = v.substring(0, v.length() - 1);
        // String[] p = v.split(",");
        // List<String> l = new ArrayList<>();
        // for (String x : p) {
        // String cleaned = x.trim();
        // while (cleaned.startsWith("\""))
        // cleaned = cleaned.substring(1);
        // while (cleaned.endsWith("\""))
        // cleaned = cleaned.substring(0, cleaned.length() - 1);
        // if (!cleaned.isEmpty())
        // l.add(cleaned);
        // }
        // dec(l);
        // System.out.println("done");
        // }

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
        l.add((int) ((len / (BASE * BASE)) % BASE));
        l.add((int) ((len / BASE) % BASE));
        l.add((int) (len % BASE));

        java.math.BigInteger BIG_BASE = java.math.BigInteger.valueOf(BASE);
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
        while (l.size() < cap)
            l.add(0);
        List<String> stringList = new ArrayList<>();
        for (int val : l) {
            stringList.add(idMap[val]);
        }
        return stringList;
    }

    public static void dec(List<String> la) throws IOException {
        List<Integer> l = new ArrayList<>();
        for (String s : la) {
            String trimmed = s.trim();
            Integer val = reverseMap.get(trimmed);
            if (val == null) {
                System.err.println("unknown toekn skipped: [" + trimmed + "]");
            } else
                l.add(val);
        }
        int idx = 0;
        int el = l.get(idx++);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < el; i++) {
            sb.append((char) (int) (l.get(idx++)));
        }
        String ex = sb.toString();
        long target = (long) l.get(idx++) * BASE * BASE
                + (long) l.get(idx++) * BASE
                + (long) l.get(idx++);
        java.math.BigInteger BIG_BASE = java.math.BigInteger.valueOf(BASE);
        java.math.BigInteger num = java.math.BigInteger.ZERO;
        for (int i = idx; i < l.size(); i++)
            num = num.multiply(BIG_BASE).add(java.math.BigInteger.valueOf(l.get(i)));
        byte[] r = new byte[(int) target];
        for (int i = (int) target - 1; i >= 0; i--) {
            java.math.BigInteger[] divRem = num.divideAndRemainder(java.math.BigInteger.valueOf(256));
            r[i] = divRem[1].byteValue();
            num = divRem[0];
        }
        Files.write(Paths.get("output." + ex), r);
    }

}
