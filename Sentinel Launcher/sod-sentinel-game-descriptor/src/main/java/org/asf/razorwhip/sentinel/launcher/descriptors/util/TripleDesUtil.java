package org.asf.razorwhip.sentinel.launcher.descriptors.util;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * 
 * Simple utility to encrypt/decrypt with triple DES, SoD uses triple des for
 * some things
 * 
 * @author Sky Swimmer
 *
 */
public class TripleDesUtil {

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Encrypts data with triple DES
	 * 
	 * @param data Data to encrypt
	 * @param key  Key to use
	 * @return Encrypted data
	 * @throws IOException If encrypting fails
	 */
	public static byte[] encrypt(byte[] data, byte[] key) throws IOException {
		try {
			Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS7Padding", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "DESede"));
			return cipher.doFinal(data);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException
				| IllegalBlockSizeException | BadPaddingException e) {
			throw new IOException("Encryption failure", e);
		}
	}

	/**
	 * Decrypts data with triple DES
	 * 
	 * @param data Data to decrypt
	 * @param key  Key to use
	 * @return Encrypted data
	 * @throws IOException If decrypting fails
	 */
	public static byte[] decrypt(byte[] data, byte[] key) throws IOException {
		try {
			Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS7Padding", "BC");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DESede"));
			return cipher.doFinal(data);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException
				| IllegalBlockSizeException | BadPaddingException e) {
			// Try without padding
			try {
				Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding", "BC");
				cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DESede"));
				return cipher.doFinal(data);
			} catch (NoSuchAlgorithmException | NoSuchProviderException | NoSuchPaddingException | InvalidKeyException
					| IllegalBlockSizeException | BadPaddingException e3) {
				throw new IOException("Decryption failure", e3);
			}
		}
	}

}
