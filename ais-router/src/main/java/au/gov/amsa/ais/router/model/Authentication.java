package au.gov.amsa.ais.router.model;

import com.github.davidmoten.guavamini.Preconditions;

public final class Authentication {

    private final String username;
    private final String password;

    private Authentication(String username, String password) {
        Preconditions.checkNotNull(username);
        Preconditions.checkNotNull(password);
        this.username = username;
        this.password = password;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String username;
        private String password;

        private Builder() {
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Authentication build() {
            return new Authentication(username, password);
        }
    }
}
