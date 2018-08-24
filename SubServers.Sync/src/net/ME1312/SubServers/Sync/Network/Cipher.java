package net.ME1312.SubServers.Sync.Network;

import net.ME1312.SubServers.Sync.Library.Config.YAMLSection;
import org.msgpack.value.Value;

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
    Value encrypt(String key, YAMLSection data) throws Exception;

    /**
     * Decrypt Encrypted JSON Data
     *
     * @param key Key to Decrypt Data with
     * @param data Encrypted Data Array
     * @return JSON Data
     */
    YAMLSection decrypt(String key, Value data) throws Exception;
}
