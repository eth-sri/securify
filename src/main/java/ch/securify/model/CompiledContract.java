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


package ch.securify.model;

import com.google.gson.annotations.SerializedName;

public class CompiledContract {

	@SerializedName("bin-runtime")
	private String bin;

	@SerializedName("srcmap-runtime")
	private String srcmap;

	public String getBinary() {
		return bin;
	}

	public String getSrcmap() {
		return srcmap;
	}
}
