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

import sys
import abc
import pathlib
import subprocess
import logging
import json

from . import utils

class Project(metaclass=abc.ABCMeta):
    """Abstract Project implemented by all different kinds projects requiring compilation and reporting."""

    def __init__(self, project_root):
        """Sets the project root."""
        self._project_root = pathlib.Path(project_root)

    def execute(self):
        """Execute the project. This includes compilation and reporting.

        This function returns 0 if no violations are found, and 1 otherwise.
        """
        logging.info("Compiling project")
        self.compile_()
        logging.info("Running Securify")
        self.run_securify()
        logging.info("Generating report")
        return self.report()

    def get_project_root(self):
        """Returns the project root of the project."""
        return self._project_root

    def run_securify(self):
        """Runs the securify command."""
        cmd = ["java", "-Xmx8G", "-jar", "/securify_jar/securify.jar", "-co", self.get_compilation_output(),
               "-o", self.get_securify_target_output()]
        process = subprocess.run(cmd, shell=False, stdout=subprocess.PIPE)
        if process.returncode:
            utils.log_error("Error running securify.")
            utils.handle_process_output_and_exit(process)

    def get_compilation_output(self):
        """Returns the hex source resulting from the compilation."""
        return pathlib.Path("/comp.json")

    def get_securify_target_output(self):
        """Returns the target file where the securify output is stored."""
        return pathlib.Path("/securify_res.json")

    @abc.abstractmethod
    def compile_(self):
        """Compile the project."""
        pass

    # @abc.abstractmethod
    def report(self):
        """Report findings.

        This function returns 0 if no violations are found, and 1 otherwise.
        """
        return_code = 0
        self.json_report = self.get_json_report()
        for contract_name, contract in self.json_report.items():
            for pattern_name, pattern in contract["results"].items():
                for token_num in pattern["violations"]:
                    utils.log_error(f"Violation in contract '{contract_name.split('/')[-1]}(token {token_num})' for " +\
                                    f"pattern '{pattern_name}'.")
                    return_code = 1
                for token_num in pattern["warnings"]:
                    utils.log_warning(f"Warning in contract '{contract_name.split('/')[-1]}(token {token_num})' for " +\
                                      f"pattern '{pattern_name}'.")
        return return_code

    def get_json_report(self):
        """Loads the JSON output from Securify."""
        with open(self.get_securify_target_output(), mode='r') as file:
            return json.load(file)
