package MerkleTree;

import java.util.Arrays;

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
}

