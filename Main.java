import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    static int cap = 256 * 384;
    static int b = 11;
    static int msk = 0x7FF;

    public static void main(String[] args) throws Exception {
        Scanner s = new Scanner(System.in);
        System.out.println("1:enc 2:dec");
        int c = s.nextInt();

        if (c == 1) {
            System.out.println("file:");
            String f = s.next();
            List<Integer> r = enc(f);
            int last = r.size() - 1;
            while (last >= 0 && r.get(last) == 0) last--;
            System.out.print("[");
            for (int i = 0; i <= last; i++) System.out.print(r.get(i) + (i == last ? "" : ", "));
            System.out.println("]");
        } else {
            System.out.println("list:");
            s.nextLine();
            String in = s.nextLine();
            String v = in.replace("[", "").replace("]", "").replace(" ", "");
            String[] p = v.split(",");
            List<Integer> l = new ArrayList<>();
            for (String x : p) l.add(Integer.parseInt(x));
            dec(l);
            System.out.println("done");
        }
    }

    public static List<Integer> enc(String f) throws IOException {
        byte[] by = Files.readAllBytes(Paths.get(f));
        String ex = f.substring(f.lastIndexOf(".") + 1);
        List<Integer> l = new ArrayList<>();
        
        l.add(ex.length());
        for (char ch : ex.toCharArray()) l.add((int) ch);
        
        long len = by.length;
        l.add((int)((len >> 22) & msk));
        l.add((int)((len >> 11) & msk)); 
        l.add((int)(len & msk));

        int buf = 0, n = 0;
        for (byte v : by) {
            buf = (buf << 8) | (v & 0xFF);
            n += 8;
            while (n >= b) {
                l.add((buf >> (n - b)) & msk);
                n -= b;
            }
        }
        if (n > 0) l.add((buf << (b - n)) & msk);
        while (l.size() < cap) l.add(0);
        return l;
    }

        public static void dec(List<Integer> l) throws IOException {
        int idx = 0; // Use a pointer instead of removing elements
        int el = l.get(idx++); 
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < el; i++) sb.append((char) (int) l.get(idx++));
        String ex = sb.toString();

        long target = ((long)l.get(idx++) << 22) | ((long)l.get(idx++) << 11) | l.get(idx++);
        byte[] r = new byte[(int)target];
        
        int buf = 0, n = 0, k = 0;
        // Start processing from where the header left off (idx)
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
