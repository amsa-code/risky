package au.gov.amsa.ais.router.model;

public final class User {

    private final String username;
    private final String passwordHash;
    private final String passwordSalt;

    private User(String username, String passwordHash, String passwordSalt) {
        Util.verifyNotBlank("username", username);
        Util.verifyNotBlank("passwordHash", passwordHash);
        Util.verifyNotBlank("passwordSalt", passwordSalt);
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String username;
        private String passwordHash;
        private String passwordSalt;

        private Builder() {
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public Builder passwordSalt(String passwordSalt) {
            this.passwordSalt = passwordSalt;
            return this;
        }

        public User build() {
            return new User(username, passwordHash, passwordSalt);
        }
    }
}
