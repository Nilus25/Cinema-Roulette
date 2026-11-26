package com.imdbselector.infrastructure.adapter.out.security;

import com.imdbselector.application.port.out.PasswordHasherPort;
import org.mindrot.jbcrypt.BCrypt;

public final class BcryptPasswordHasher implements PasswordHasherPort {
    @Override
    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return BCrypt.checkpw(rawPassword, passwordHash);
    }
}
