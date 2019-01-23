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


package ch.securify.analysis;

import ch.securify.decompiler.instructions.Instruction;
import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DataflowFactory {

	private static Function<List<Instruction>, AbstractDataflow> dataflowGenerator;

	private static Map<String, Function<List<Instruction>, AbstractDataflow>> dataflowGenerators = new HashMap<>();
	static {
		// Default dataflow
		dataflowGenerators.put("default", Dataflow::new);

		setDataflowInstanceClass(null);
	}

	public static void setDataflowInstanceClass(String dataflowClass) {
		if (Strings.isNullOrEmpty(dataflowClass)) {
			dataflowGenerator = dataflowGenerators.get("default");
		}
		else if (dataflowGenerators.keySet().contains(dataflowClass.toLowerCase())) {
			dataflowGenerator = dataflowGenerators.get(dataflowClass.toLowerCase());
		}
		else {
			throw new IllegalArgumentException("Invalid dataflow class name: " + dataflowClass);
		}
	}

	public static AbstractDataflow getDataflow(List<Instruction> decompiledInstructions) {
		return dataflowGenerator.apply(decompiledInstructions);
	}

}
