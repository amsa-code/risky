package au.gov.amsa.risky.format;

import rx.Observable.Transformer;

public final class Fixes {

    public static <T extends Fix> Transformer<T, T> ignoreOutOfOrderFixes(final boolean enabled) {
        return o -> {
            return o.scan((a, b) -> {
                if (!enabled || b.time() > a.time())
                    return b;
                else
                    return a;
            }).distinctUntilChanged();
        };
    }

}
