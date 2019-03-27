/*
 *  Copyright 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


package ch.securify.utils;

import java.math.BigInteger;

public class BigIntUtil {


	/**
	 * Convert an int256 (big-endian byte array) to a BigInteger.
	 * @param int256
	 * @return
	 */
	public static BigInteger fromInt256(byte[] int256) {
		int sig = (int256.length == 32 && int256[0] < 0) ? -1 : 1;
		return new BigInteger(sig, int256);
	}

	public static BigInteger fromUint256(byte[] uint256) {
		return new BigInteger(1, uint256);
	}


	/**
	 * Convert a BigInteger to an int256 (big-endian byte array).
	 * @param bigInt
	 * @return
	 */
	public static byte[] toInt256(BigInteger bigInt) {
		byte[] bytes = bigInt.toByteArray();
		if (bytes.length <= 32) {
			return bytes;
		}
		byte[] int256 = new byte[32];
		System.arraycopy(bytes, bytes.length - 32, int256, 0, 32);
		return int256;
	}


}
