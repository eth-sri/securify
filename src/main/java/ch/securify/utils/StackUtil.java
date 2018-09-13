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

import java.util.Arrays;
import java.util.Stack;

public class StackUtil {


	/**
	 * Makes a shallow copy of the stack.
	 * @param stack
	 * @param <T>
	 * @return
	 */
	public static <T> Stack<T> copyStack(Stack<T> stack) {
		Stack<T> copy = new Stack<>();
		copy.addAll(stack);
		return copy;
	}


	/**
	 * Push an array of items onto the stack.
	 * The last array item will be pushed last, thus ending up at the top of the stack.
	 * @param stack
	 * @param items
	 * @param <T>
	 */
	public static <T> void pushAll(Stack<T> stack, T[] items) {
		Arrays.stream(items).forEachOrdered(stack::push);
	}


	/**
	 * Push an array of items onto the stack.
	 * The first array item will be pushed last, thus ending up at the top of the stack.
	 * @param stack
	 * @param items
	 * @param <T>
	 */
	public static <T> void pushAllRev(Stack<T> stack, T[] items) {
		for (int i = items.length - 1; i >= 0; --i) {
			stack.push(items[i]);
		}
	}


	/**
	 * Remove a number of items from the stack.
	 * @param stack
	 * @param n number of items to pop.
	 * @param <T>
	 */
	public static <T> void pop(Stack<T> stack, int n) {
		while (n --> 0) stack.pop();
	}


	/**
	 * Create a new Stack with the given items.
	 * @param items the first element is pushed first, i.e. it will end up at the bottom of the Stack.
	 * @param <T>
	 * @return Stack containing the specified items.
	 */
	@SafeVarargs
	public static <T> Stack<T> create(T... items) {
		Stack<T> stack = new Stack<>();
		for (T item : items) {
			stack.push(item);
		}
		return stack;
	}


}
