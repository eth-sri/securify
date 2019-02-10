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
import json
import logging
import os
import pathlib
import subprocess

from . import utils
from . import project


class TruffleProject(project.Project):
    """A project that uses the truffle development environment to compile the
    project."""

    def __init__(self, project_root, args, securify_flags):
        super().__init__(project_root, args, securify_flags)
        self.build_dir = self.project_root / pathlib.Path("build/contracts/")

    def compile_(self, compilation_output):
        with utils.working_directory(self.project_root):
            try:
                output = subprocess.check_output(["truffle", "compile"],
                                                 universal_newlines=True,
                                                 stderr=subprocess.STDOUT)
                logging.debug(output)
            except subprocess.CalledProcessError as e:
                logging.error("Error compiling Truffle project")
                utils.handle_process_output_and_exit(e)

        self._merge_compiled_files(compilation_output)

    def _merge_compiled_files(self, compilation_output):
        """Merges individual truffle files into an aggregate file for
        securify."""
        result = {}
        for entry in os.scandir(self.build_dir):
            if entry.is_file() and entry.name.endswith(".json") and\
                    entry.name != "Migrations.json":
                with open(entry) as f:
                    data = json.load(f)
                contract_path = data["sourcePath"]
                contract_name = data["contractName"]
                # check if library contract
                if not pathlib.Path(contract_path).is_file():
                    node_modules_dir = utils.find_node_modules_dir(
                        self.project_root)
                    if node_modules_dir is None:
                        logging.error("Couldn't find a source for "
                                      f"{contract_name}")
                        sys.exit(1)
                    contract_path = os.path.join(node_modules_dir,
                                                 contract_path)

                    if not pathlib.Path(contract_path).is_file():
                        logging.error("Couldn't find a source for "
                                      f"{contract_name}")
                        sys.exit(1)

                data["bin"] = data.pop("bytecode")
                data["bin-runtime"] = data.pop("deployedBytecode")
                data["srcmap"] = data.pop("sourceMap")
                data["srcmap-runtime"] = data.pop("deployedSourceMap")
                # remove leading '0x'
                data["bin"] = data["bin"][2:]
                data["bin-runtime"] = data["bin-runtime"][2:]

                result[f'{contract_path}:{contract_name}'] = data

        # dump aggregate compiled output to file
        with open(compilation_output, 'w') as file:
            json.dump(result, file, indent=4)
