package au.gov.amsa.util;

public final class SixBit {

    /** Precompiled list of int to six bit mappings. */
    private static final int[] INT_TO_SIX_BIT = createIntToSixBit();

    private static int[] createIntToSixBit() {
        int[] toSixbit = new int[256 * 256]; // we actually only use 256, but we
                                             // parse chars around instead of
                                             // bytes
        for (int chr = 0; chr < toSixbit.length; chr++) {
            if (chr < 48 || chr > 119 || chr > 87 && chr < 96) {
                toSixbit[chr] = -1;
            } else if (chr < 0x60) {
                toSixbit[chr] = chr - 48 & 0x3F;
            } else {
                toSixbit[chr] = chr - 56 & 0x3F;
            }
        }
        return toSixbit;
    }

    /**
     * Append bits from a sixbit encoded string
     * 
     * @param str
     * @param padBits
     */
    public static boolean[] sixBitToBits(String str, int padBits) {
        if (str.length() == 0) {
            return new boolean[0];
        }
        int len = str.length() * 6 - padBits;
        boolean[] bitSet = new boolean[len];

        int slen = str.length() - 1;
        int index = 0;
        for (int i = 0; i < slen; i++) {
            char chr = str.charAt(i);
            int binVal = INT_TO_SIX_BIT[chr];
            if (binVal == -1) {
                throw new RuntimeException("Illegal sixbit ascii char: " + chr);
            }
            bitSet[index] = (binVal & 32) > 0;
            bitSet[index + 1] = (binVal & 16) > 0;
            bitSet[index + 2] = (binVal & 8) > 0;
            bitSet[index + 3] = (binVal & 4) > 0;
            bitSet[index + 4] = (binVal & 2) > 0;
            bitSet[index + 5] = (binVal & 1) > 0;
            index += 6;
        }

        // Process the last char which might be padded
        char chr = str.charAt(slen);
        int binVal = INT_TO_SIX_BIT[chr];
        if (binVal == -1) {
            throw new RuntimeException("Illegal sixbit ascii char: " + chr);
        }
        int bits = 6 - padBits;
        switch (bits) {
        case 6:
            bitSet[index + 5] = (binVal & 1) > 0;
        case 5:
            bitSet[index + 4] = (binVal & 2) > 0;
        case 4:
            bitSet[index + 3] = (binVal & 4) > 0;
        case 3:
            bitSet[index + 2] = (binVal & 8) > 0;
        case 2:
            bitSet[index + 1] = (binVal & 16) > 0;
        case 1:
            bitSet[index] = (binVal & 32) > 0;
        }
        return bitSet;
    }

    public static long getValue(int from, int to, boolean[] bitSet) {
        if (to > bitSet.length) {
            throw new RuntimeException(bitSet.length + " is not enough bits. At least " + to
                    + " expected.");
        }
        long val = 0;
        long powMask = 1;
        for (int i = to - 1; i >= from; i--) {
            if (bitSet[i]) {
                val += powMask;
            }
            powMask <<= 1;
        }
        return val;
    }

    public static long getSignedValue(int from, int to, boolean[] bitSet) {
        if (to > bitSet.length) {
            throw new RuntimeException(bitSet.length + " is not enough bits. At least " + to
                    + " expected.");
        }
        long val = 0;
        long powMask = 1;

        for (int i = to - 1; i >= from; i--) {
            if (bitSet[i]) {
                val += powMask;
            }
            powMask <<= 1;
        }
        if (bitSet[from]) {
            val = val - powMask;
        }
        return val;
    }

    public static String getString(int from, int to, boolean[] bitSet) {
        int len = (to - from) / 6;
        char[] resStr = new char[len];
        int pos = from;
        int i;
        for (i = 0; i < len; i++) {
            char ch = (char) intToascii((char) SixBit.getValue(pos, pos + 6, bitSet));
            if (ch == '@') {
                len = i;
                break;
            }
            resStr[i] = ch;
            pos += 6;
        }
        // remove trailing @ characters and spaces
        while (len > 0 && (resStr[len - 1] == ' '))
            len -= 1;
        return new String(resStr, 0, len);
    }

    /**
     * Convert six bit int value to character
     *
     * @param val
     * @return
     */
    public static int intToascii(int val) {
        if (val > 63) {
            throw new RuntimeException("Char value " + val + " not allowed");
        } else if (val < 32) {
            return val + 64;
        } else {
            return val;
        }
    }

}
