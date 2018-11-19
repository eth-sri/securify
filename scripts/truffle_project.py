#!/usr/bin/env python3

"""
Author: Jakob Beckmann

Copyright 2018 ChainSecurity AG

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import os
import json
import subprocess
import pathlib

from . import utils
from . import project


class TruffleProject(project.Project):
    """A project that uses the truffle development environment to compile the project."""

    def __init__(self, project_root):
        super().__init__(project_root)
        self.build_dir = self.project_root / pathlib.Path("build/contracts/")

    def compile_(self):
        with utils.working_directory(self.project_root):
            try:
                subprocess.check_output(["truffle", "compile"],
                                        shell=False,
                                        stderr=subprocess.STDOUT)
            except subprocess.CalledProcessError as e:
                utils.log_error("Error compiling truffle project.")
                utils.handle_process_output_and_exit(e)

        self._merge_compiled_files()

    def _merge_compiled_files(self):
        """Merges individual truffle files into an aggregate file for securify."""
        result = {}
        for entry in os.scandir(self.build_dir):
            if entry.is_file() and entry.name.endswith(".json") and entry.name != "Migrations.json":
                with open(entry) as file:
                    data = json.load(file)
                contract_name = data["sourcePath"] + ":" + data["contractName"]
                # check if library contract
                if not pathlib.Path(contract_name).is_file():
                    contract_name = os.path.join(
                        utils.find_node_modules_dir(self.project_root),
                        contract_name)

                data["bin"] = data.pop("bytecode")
                data["bin-runtime"] = data.pop("deployedBytecode")
                data["srcmap"] = data.pop("sourceMap")
                data["srcmap-runtime"] = data.pop("deployedSourceMap")
                # remove leading '0x'
                data["bin"] = data["bin"][2:]
                data["bin-runtime"] = data["bin-runtime"][2:]

                result[contract_name] = data

        # dump aggregate compiled output to file
        with open(self.compilation_output, mode='w') as file:
            json.dump(result, file, indent=4)
