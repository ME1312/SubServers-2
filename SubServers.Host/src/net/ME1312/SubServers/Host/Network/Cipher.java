package net.ME1312.SubServers.Host.Network;

import org.json.JSONObject;

/**
 * SubData Cipher Layout Class
 */
public interface Cipher {
    /**
     * Get the name of this Cipher
     *
     * @return Cipher Name
     */
    String getName();

    /**
     * Encrypt JSON Data
     *
     * @param key Key to Encrypt Data with
     * @param data Data to Encrypt
     * @return Encrypted Data Array
     */
    byte[] encrypt(String key, JSONObject data) throws Exception;

    /**
     * Decrypt Encrypted JSON Data
     *
     * @param key Key to Decrypt Data with
     * @param data Encrypted Data Array
     * @return JSON Data
     */
    JSONObject decrypt(String key, byte[] data) throws Exception;
}
