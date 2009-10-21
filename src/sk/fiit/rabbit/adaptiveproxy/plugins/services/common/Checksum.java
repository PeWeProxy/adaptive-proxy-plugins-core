package sk.fiit.rabbit.adaptiveproxy.plugins.services.common;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {
	public static String md5(String text) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(text.getBytes());
			BigInteger hash = new BigInteger(1, md5.digest());
			String hashword = hash.toString(16);

			StringBuffer b = new StringBuffer(hashword);

			while (b.length() < 32) {
				b.insert(0, '0');
			}

			return b.toString();

		} catch (NoSuchAlgorithmException e) {
			// nemalo by sa stat
			throw new RuntimeException("MD5 Algorithm not found");
		}
	}
}
