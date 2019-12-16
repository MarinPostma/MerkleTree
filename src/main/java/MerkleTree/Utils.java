package MerkleTree;

import java.util.Arrays;
import java.security.MessageDigest;

public class Utils {
	public static String byteToHex(byte num) {
		char[] hexDigits = new char[2];
		hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
		hexDigits[1] = Character.forDigit((num & 0xF), 16);
		return new String(hexDigits);
	}

	public static String encodeHexString(byte[] byteArray, int sliceLen) {
		StringBuffer hexStringBuffer = new StringBuffer();
		byte[] slice = Arrays.copyOfRange(byteArray, byteArray.length - sliceLen, byteArray.length);
		for (int i = 0; i < slice.length; i++) {
			hexStringBuffer.append(byteToHex(byteArray[i]));
		}
		return hexStringBuffer.toString();
	}

	public static byte[] hash(byte[] val) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(val);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] concat(byte[] first, byte[] second) {
		byte[] concat = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, concat, first.length, second.length);
		return concat;
	}
}

