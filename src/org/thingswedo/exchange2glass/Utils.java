package org.thingswedo.exchange2glass;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import com.google.api.client.util.Base64;

public class Utils {
	private static final String ENC_PASS = "qazwsx123456^%$#@!";
	private static final String ENC_SALT = "!@QWE123";

	public static String encodePassword(String password) {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory
					.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(ENC_PASS
					.toCharArray()));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(
					ENC_SALT.getBytes(), 20));
			return Base64.encodeBase64String(pbeCipher.doFinal(password
					.getBytes("UTF-8")));
		} catch (Exception e) {
			throw new RuntimeException("Cannot encrypt password", e);
		}
	}

	public static String decodePassword(String password) {
		try {
			SecretKeyFactory keyFactory = SecretKeyFactory
					.getInstance("PBEWithMD5AndDES");
			SecretKey key = keyFactory.generateSecret(new PBEKeySpec(ENC_PASS
					.toCharArray()));
			Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
			pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(ENC_SALT.getBytes(),
					20));
			return new String(pbeCipher.doFinal(Base64.decodeBase64(password)),
					"UTF-8");
		} catch (Exception e) {
			throw new RuntimeException("Cannot decrypt password", e);
		}
	}

	public static void main(String[] args){
		String pass = "BakaeBay0613&";
		String enc = encodePassword(pass);
		System.out.println(enc);
		System.out.println(decodePassword(enc));;
	}
}
