package org.example;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class Main {
    static int cap = 256 * 384;
    static int b = 10;
    static int msk = (1 << b) - 1;
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
    }

    public static void main(String[] args) throws Exception {
        loadJson();
        Scanner s = new Scanner(System.in);
        System.out.println("1:enc 2:dec");
        int c = s.nextInt();

        if (c == 1) {
            System.out.println("file:");
            String f = "app/src/main/resources/" + s.next();
            List<String> r = enc(f);
            int last = r.size() - 1;
            while (last >= 0 && r.get(last).equals(idMap[0]))
                last--;
            System.out.print("[");
            for (int i = 0; i <= last; i++)
                System.out.print("\"" + r.get(i) + "\"" + (i == last ? "" : ", "));
            System.out.println("]");
        } else {
            System.out.println("list:");
            s.nextLine();
            String in = s.nextLine();
            String v = in.trim();
            if (v.startsWith("["))
                v = v.substring(1);
            if (v.endsWith("]"))
                v = v.substring(0, v.length() - 1);
            String[] p = v.split(",");
            List<String> l = new ArrayList<>();
            for (String x : p) {
                String cleaned = x.trim();
                while (cleaned.startsWith("\""))
                    cleaned = cleaned.substring(1);
                while (cleaned.endsWith("\""))
                    cleaned = cleaned.substring(0, cleaned.length() - 1);
                if (!cleaned.isEmpty())
                    l.add(cleaned);
            }
            dec(l);
            System.out.println("done");
        }
    }

    public static List<String> enc(String f) throws IOException {
        byte[] by = Files.readAllBytes(Paths.get(f));
        String filenameOnly = Paths.get(f).getFileName().toString();
        String ex = filenameOnly.substring(filenameOnly.lastIndexOf(".") + 1);
        List<Integer> l = new ArrayList<>();
        l.add(ex.length() & msk);
        for (char ch : ex.toCharArray())
            l.add((int) ch & msk);
        long len = by.length;
        l.add((int) ((len >> 20) & msk));
        l.add((int) ((len >> 10) & msk));
        l.add((int) (len & msk));

        int buf = 0, n = 0;
        for (byte v : by) {
            buf = (buf << 8) | (v & 0xFF);
            n += 8;
            while (n >= b) {
                l.add((buf >> (n - b)) & msk);
                n -= b;
            }
        }
        if (n > 0)
            l.add((buf << (b - n)) & msk);
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
        int el = l.get(idx++) & msk;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < el; i++) {
            sb.append((char) (int) (l.get(idx++) & msk));
        }
        String ex = sb.toString();
        long target = ((long) (l.get(idx++) & msk) << 20) | ((long) (l.get(idx++) & msk) << 10)
                | (long) (l.get(idx++) & msk);
        byte[] r = new byte[(int) target];
        int buf = 0, n = 0, k = 0;
        for (int i = idx; i < l.size() && k < target; i++) {
            buf = (buf << b) | (l.get(i) & msk);
            n += b;
            while (n >= 8 && k < target) {
                r[k++] = (byte) ((buf >> (n - 8)) & 0xFF);
                n -= 8;
            }
        }
        Files.write(Paths.get("output." + ex), r);
    }

}
