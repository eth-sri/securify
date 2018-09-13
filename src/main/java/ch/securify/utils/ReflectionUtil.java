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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class ReflectionUtil {


	/**
	 * Get a constant's name by its value in a class.
	 * @param cls
	 * @param value
	 * @return constant's name, null if nothing found
	 */
	public static String getConstantNameByValue(Class<?> cls, Object value) {
		for (Field f : cls.getDeclaredFields()) {
			int mod = f.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
				try {
					if (Objects.equals(f.get(null), value)) {
						return f.getName();
					}
				}
				catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}


}
