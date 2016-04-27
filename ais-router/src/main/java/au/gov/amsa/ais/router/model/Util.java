package au.gov.amsa.ais.router.model;

import com.github.davidmoten.guavamini.Preconditions;

class Util {
    static void verifyId(String id) {
        Preconditions.checkNotNull(id);
        Preconditions.checkArgument(id.trim().length() > 0, "id cannot be blank");
    }
}
