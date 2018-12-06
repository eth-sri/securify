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
import contextlib
import logging
import os
import re
import sys

from solc.exceptions import SolcError
import solc.install


class NoSolidityProject(Exception):
    def __init__(self, folder):
        super().__init__()
        self.folder = folder


class CompilerVersionNotSupported(Exception):
    def __init__(self, version, too_old=True):
        super().__init__()
        self.version = version
        self.too_old = too_old


class SolidityCompilationException(SolcError):
    def __init__(self, solc_exception, files):
        super().__init__(solc_exception.command,
                         solc_exception.return_code,
                         solc_exception.stdin_data,
                         solc_exception.stderr_data,
                         solc_exception.stdout_data,
                         solc_exception.message)
        self.files = files


def version_to_tuple(v):
    """Converts a version string into a tuple."""
    return tuple(map(int, v.split('.')))


def find_node_modules_dir(directory):
    """Returns the path to the node module directory contained in the
    `directory` directory."""
    for x in os.walk(directory):
        if os.path.isdir(os.path.join(x[0], 'node_modules')):
            return os.path.join(x[0], 'node_modules')
    return None


def parse_sol_version(source):
    """Parses the solidity version from a contract using the pragma."""
    with open(source, encoding='utf-8') as f:
        lines = f.readlines()

    for l in lines:
        if 'pragma' in l and not 'experimental' in l:
            if '^' in l or '>' in l:
                return DEFAULT_SOLC_VERSION

            match = COMP_VERSION1_REX.search(l)
            if match is None:
                raise RuntimeError('Could not parse compiler pragma.')

            solc_version = match.group(0)
            if solc_version not in SOLC_VERSIONS:
                raise CompilerVersionNotSupported(
                    solc_version, solc_version < SOLC_VERSIONS[0])

            return solc_version

    return DEFAULT_SOLC_VERSION


def handle_process_output_and_exit(error):
    """Processes stderr from a process error."""
    if error.stderr:
        logging.fatal(error.stderr.strip())
    sys.exit(1)


def set_logger_level(level=None):
    if level == "info":
        log_level = logging.INFO
    elif level == "error":
        log_level = logging.ERROR
    else:
        log_level = logging.WARNING

    logging.basicConfig(format="%(message)s", level=log_level)


@contextlib.contextmanager
def working_directory(path):
    prev_cwd = os.getcwd()
    os.chdir(path)
    try:
        yield
    finally:
        os.chdir(prev_cwd)


COMP_VERSION1_REX = re.compile(r'0\.\d+\.\d+')

OUTPUT_VALUES = ('abi',
                 'ast',
                 'bin-runtime',
                 'srcmap-runtime')

_VERSIONS = (getattr(solc.install, item)[1:]
             for item in dir(solc.install) if item.startswith('V'))

SOLC_VERSIONS = tuple(filter(
    lambda x: version_to_tuple(x) >= version_to_tuple('0.4.11'),
    _VERSIONS))

DEFAULT_SOLC_VERSION = SOLC_VERSIONS[-1]
