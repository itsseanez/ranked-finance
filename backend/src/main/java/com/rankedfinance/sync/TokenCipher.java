package com.rankedfinance.sync;

/**
 * Encrypts provider secrets (Plaid access tokens, SimpleFIN access URLs) before they go in the
 * database. Today's implementation uses a key from the environment; a KMS-backed one can replace it
 * behind this interface (ADR 0016).
 */
public interface TokenCipher {

    /** Returns the bytes to store. Throws IllegalStateException if no key is configured. */
    byte[] encrypt(String plaintext);

    /**
     * Reverses {@link #encrypt}. Throws IllegalStateException if the value was altered or is
     * unreadable.
     */
    String decrypt(byte[] stored);
}
