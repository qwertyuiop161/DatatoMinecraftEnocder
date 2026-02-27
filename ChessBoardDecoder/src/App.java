public class App {

    public static void main(String[] args) {
        int[] array = {0,0,1,0,0,1,1,0,1};
        array=truncate(array);
        for (int i =0; i<array.length; i++) {
            System.out.println(array[i]);
        }
    }
    public static byte decodeBits(int[] bits) {
        byte b = 0;
        if (bits.length%8!=0) {
            truncate(bits);
        }
        if (bits.length==8) {
            for (int i = 0; i<8;i++) {
                int position = bits.length-1-i;
                int shiftedbit=bits[i]<<position;
                b|=(byte)shiftedbit;
            }
            return b;
        }
        
    }
    public static int[] truncate(int[] input) {
        if (input == null || input.length < 8) {
            return new int[0];
        }

        int remainder = input.length % 8;
        if (remainder == 0) {
            return input;
        }

        int newSize = input.length - remainder;
        int[] result = new int[newSize];
    
        System.arraycopy(input, remainder, result, 0, newSize);
    
        return result;
}

}
