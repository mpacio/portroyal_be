package com.matteopaciolla.prbe.security;

import java.io.Serializable;

public record CredentialVersion(Long userId, long version) implements Serializable {
}
