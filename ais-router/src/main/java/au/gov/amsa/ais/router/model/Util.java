package au.gov.amsa.ais.router.model;

import com.github.davidmoten.guavamini.Preconditions;

final class Util {

    static void verifyId(String id) {
        verifyNotBlank("id", id);
    }

    static void verifyNotBlank(String name, String s) {
        Preconditions.checkNotNull(s);
        Preconditions.checkArgument(s.trim().length() > 0, name + " cannot be null or blank");
    }
}
