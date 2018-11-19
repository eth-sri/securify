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
import abc
import json
import logging
import pathlib
import psutil
import subprocess
import sys

from . import utils


class Project(metaclass=abc.ABCMeta):
    """Abstract Project implemented by all different kinds projects requiring compilation and reporting."""

    compilation_output = pathlib.Path("/comp.json")
    securify_target_output = pathlib.Path("/securify_res.json")
    securify_jar = pathlib.Path("/securify_jar/securify.jar")

    def __init__(self, project_root):
        """Sets the project root."""
        self.project_root = pathlib.Path(project_root)

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

    def run_securify(self):
        """Runs the securify command."""
        memory = psutil.virtual_memory().available // 1024 ** 3
        cmd = ["java", f"-Xmx{memory}G", "-jar", str(self.securify_jar),
               "-co", self.compilation_output,
               "-o", self.securify_target_output]
        try:
            subprocess.check_output(cmd, shell=False, stderr=subprocess.STDOUT)
        except subprocess.CalledProcessError as e:
            logging.error("Error running securify.")
            utils.handle_process_output_and_exit(e)

    @abc.abstractmethod
    def compile_(self):
        """Compile the project."""
        pass

    def report(self):
        """Report findings.

        This function returns 0 if no violations are found, and 1 otherwise.
        """
        with open(self.securify_target_output) as file:
            json_report = json.load(file)

        print(json.dumps(json_report, indent=4))

        for contract in json_report.values():
            for pattern in contract["results"].values():
                if pattern["violations"]:
                    return 1
        return 0
