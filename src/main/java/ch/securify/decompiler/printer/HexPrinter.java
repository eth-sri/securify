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


package ch.securify.decompiler.printer;

import ch.securify.utils.Hex;

import java.util.Collection;
import java.util.stream.Collectors;

public class HexPrinter {


	/**
	 * Convert an int to its hexadecimal representation.
	 * @param value input
	 * @return hex string
	 */
	public static String toHex(int value) {
		return String.format("%02X", value);
	}


	/**
	 * Convert a byte to its hexadecimal representation.
	 * @param value input
	 * @return hex string
	 */
	public static String toHex(byte value) {
		return String.format("%02X", value & 0xFF);
	}


	/**
	 * Convert a byte array to its hexadecimal representation.
	 * @param value input
	 * @return hex string
	 */
	public static String toHex(byte[] value) {
		return Hex.encode(value);
	}


	/**
	 * Convert a Collection to a list of values in its hexadecimal representation.
	 * @param collection input
	 * @return hex string
	 */
	public static String toHex(Collection<Integer> collection, String separator) {
		return collection.stream().map(HexPrinter::toHex).collect(Collectors.joining(separator));
	}


}
