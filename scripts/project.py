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
import subprocess
import tempfile

import psutil
from . import utils


def report(securify_target_output):
    """Report findings.
    """

    return 0


class Project(metaclass=abc.ABCMeta):
    """Abstract class implemented by projects using compilation and reporting.
    """
    securify_jar = pathlib.Path("build/libs/securify.jar")

    def __init__(self, project_root, args, securify_flags):
        """Sets the project root.
        """
        self.project_root = pathlib.Path(project_root)
        self.args = args
        self.securify_flags = securify_flags

    def execute(self):
        """Executes the project. This includes compilation and reporting.

        This function returns 0 if no violations are found, and 1 otherwise.
        """
        json_report = self._execute()

        if self.args.noexiterror:
            return 0

        for fname in json_report:
            for pattern in json_report[fname]["results"]:
                if len(json_report[fname]["results"][pattern]["violations"]) > 0:
                    return 1
        return 0

    def execute_for_json(self):
        """Executes the project. This includes compilation and reporting.

        This function returns the full json result.
        """
        return self._execute()

    def _execute(self):
        """Internally executes the project. This includes compilation and reporting.
        """
        with tempfile.TemporaryDirectory() as d:
            tmpdir = pathlib.Path(d)

            logging.info("Compiling project")
            compilation_output = tmpdir / "comp.json"
            self.compile_(compilation_output)

            logging.info("Running Securify")
            securify_target_output = tmpdir / "securify_res.json"
            self.run_securify(compilation_output, securify_target_output)

            with open(securify_target_output) as file:
                json_report = json.load(file)

            return json_report

    def run_securify(self, compilation_output, securify_target_output):
        """Runs the securify command.
        """
        memory = psutil.virtual_memory().available // 1024 ** 3
        cmd = ["java", f"-Xmx{memory}G", "-jar", str(self.securify_jar),
               "-co", compilation_output,
               "-o", securify_target_output]
        if self.args.json:
            cmd += ["--json"]
        if self.args.descriptions:
            cmd += ["--descriptions"]
        if self.args.verbose:
            cmd += ["-v"]
        if self.args.quiet:
            cmd += ["-q"]
        cmd += self.securify_flags

        try:
            subprocess.run(cmd, check=True, universal_newlines=True)
        except subprocess.CalledProcessError as e:
            logging.error("Error running Securify")
            utils.handle_process_output_and_exit(e)

    @abc.abstractmethod
    def compile_(self, compilation_output):
        """Compile the project.
        """
        pass
