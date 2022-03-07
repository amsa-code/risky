package au.gov.amsa.ais.parse;

public final class Util {

    private Util() {

    }

    // decimal focused optimization, digits only 
    public static int parseInt(String s) {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i);
            if (ch < '0' || ch > '9') {
                throw new NumberFormatException("not a digit: " + ch);
            }
            result = result * 10 + ch - '0';
        }
        return result;
    }

    // decimal focused optimization, digits only
    public static long parseLong(String s) {
        if (s == null) {
            throw new NumberFormatException("null");
        }
        long result = 0;
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i);
            if (ch < '0' || ch > '9') {
                throw new NumberFormatException("not a digit: " + ch);
            }
            result = result * 10 + ch - '0';
        }
        return result;
    }
}
