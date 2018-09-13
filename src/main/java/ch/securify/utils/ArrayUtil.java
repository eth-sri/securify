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

public class ArrayUtil {


	/**
	 * Find the next index (first best after current index) of an array with a non-null item.
	 * @param currIndex current index.
	 * @param array
	 * @return next array index.
	 */
	public static int nextNonNullIndex(int currIndex, Object[] array) {
		do {
			currIndex++;
		} while (currIndex < array.length && array[currIndex] == null);
		return currIndex;
	}


	/**
	 * Find the next index (first best after current index) of an array with a non-null item.
	 * @param currIndex current index.
	 * @param array
	 * @return next array index.
	 */
	public static int prevNonNullIndex(int currIndex, Object[] array) {
		do {
			currIndex--;
		} while (currIndex >= 0 && array[currIndex] == null);
		return currIndex;
	}


	/**
	 * Find the next item (first best after current index) of an array that is not null.
	 * @param currIndex current index.
	 * @param array
	 * @return next array item.
	 */
	public static <T> T nextNonNullItem(int currIndex, T[] array) {
		do {
			currIndex++;
		} while (currIndex < array.length && array[currIndex] == null);
		return currIndex < array.length ? array[currIndex] : null;
	}


	/**
	 * Find the next item (first best after current index) of an array that is not null.
	 * @param currIndex current index.
	 * @param array
	 * @return next array item.
	 */
	public static <T> T prevNonNullItem(int currIndex, T[] array) {
		do {
			currIndex--;
		} while (currIndex >= 0 && array[currIndex] == null);
		return currIndex >= 0 ? array[currIndex] : null;
	}


}
