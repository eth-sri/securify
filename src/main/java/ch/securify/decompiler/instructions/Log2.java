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


/*
 * Floern, dev@floern.com, 2017, MIT Licence
 */
package ch.securify.decompiler.instructions;

public class Log2 extends Instruction {

	@Override
	public String getStringRepresentation() {
		return "log(memoffset: " + getInput()[0] + ", length: " + getInput()[1] + ", t1: " + getInput()[2] + ", t2: " + getInput()[3] + ")";
	}

}
