#!/usr/bin/env python3

"""
Authors: Tobias Kaiser, Jakob Beckmann

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

from . import project
from . import pysolc

class SolcProject(project.Project):
    """A project that uses the `solc` compiler.
    """

    def compile_(self, compilation_output):
        """Compile the project and dump the output to an intermediate file.
        """
        sources = self._get_sol_files()

        if not sources:
            raise pysolc.NoSolidityProject(self.project_root)

        comp_output = self._compile_solfiles(sources)

        with open(compilation_output, 'w') as fs:
            json.dump(comp_output, fs)

    def _get_sol_files(self):
        """Returns the solidity files contained in the project.
        """
        return pysolc.get_sol_files(self.project_root)

    def _compile_solfiles(self, files, solc_version=None, output_values=pysolc.OUTPUT_VALUES):
        """Compiles the files using the solc compiler.
        """
        return pysolc.compile_solfiles(files, self.project_root, solc_version, output_values)


