package com.campaignorganizer.accounts.adapter.account.out.mfa;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.springframework.security.web.webauthn.api.Bytes;

/**
 * WebAuthn's {@code PublicKeyCredentialUserEntity.id} is an opaque byte handle — this app uses
 * the account's own UUID for it, so a credential's user id round-trips directly back to an
 * {@code Account} without a separate "WebAuthn user" identity/table. One canonical conversion
 * shared by every adapter that needs it, so two independent implementations can't silently
 * diverge on byte order.
 */
public final class WebAuthnUserHandle {

    private WebAuthnUserHandle() {
    }

    public static Bytes toBytes(UUID accountId) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(accountId.getMostSignificantBits());
        buffer.putLong(accountId.getLeastSignificantBits());
        return new Bytes(buffer.array());
    }

    public static UUID toAccountId(Bytes handle) {
        ByteBuffer buffer = ByteBuffer.wrap(handle.getBytes());
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
