"""
Author: Tobias Kaiser

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
import re
import operator
from collections import namedtuple
from distutils.version import StrictVersion
import sys
import json
import requests.exceptions
import subprocess

from solcx.main import _parse_compiler_output
from solcx.wrapper import solc_wrapper
from solcx.exceptions import SolcError
from solcx import get_installed_solc_versions
from solcx import get_solc_folder
from solcx import install_solc

from .utils import find_node_modules_dir

MINIMAL_SOLC_VERSION = "0.4.11"

class NoSolidityProject(BaseException):
    def __init__(self, dirpath):
        self.dir = dirpath


class CompilerVersionNotSupported(BaseException):
    pass


class SolidityCompilationException(SolcError):
    def __init__(self, solc_exception, files):
        super().__init__(
            solc_exception.command,
            solc_exception.return_code,
            solc_exception.stdin_data,
            solc_exception.stderr_data,
            solc_exception.stdout_data,
            solc_exception.message
        )
        self.files = files


class OffsetException(Exception):
    pass


OUTPUT_VALUES = ('abi',
                 'ast',
                 'bin-runtime',
                 'srcmap-runtime')

class SolidityVersion(StrictVersion):
    """Class to define a solidity version
    inherits comparators from StrictVersion and adds one for the case '^x.y.z'
    """
    def __xor__(self, other):
        return self.version[0] == other.version[0] and \
               self.version[1:] >= other.version[1:]

    def __str__(self):
        s = super(SolidityVersion, self).__str__()
        # Fix for "0.5"
        if len(s) <= 3:
            s += ".0"
        return s

OperatorVersionTuple = namedtuple('OperatorVersionTuple', ['op', 'v'])

# grouping matches into operator and version part
# e.g for >=0.4.24 it would group >= and 0.4.24
comp_version_rex = re.compile(r'(?P<operator>(<|>|>=|<=|\^)?)'
                              r'(?P<version>\d+\.\d+\.\d+)')

ops = {
    '>': operator.gt,
    '<': operator.lt,
    '': operator.eq,
    '>=': operator.ge,
    '<=': operator.le,
    '^': operator.xor
}

def _get_binary(solc_version):
    """Returns the binary for some version of solc.
    """
    binary = os.path.join(get_solc_folder(), f'solc-v{solc_version}')
    if not os.path.exists(binary):
        raise AssertionError(f'solc binary not found for version: {solc_version}')
    return binary

def get_supported_solc_versions():
    versions = [SolidityVersion(v[1:])
                for v in get_installed_solc_versions()]
    return [v for v in versions if v >= SolidityVersion(MINIMAL_SOLC_VERSION)]

def parse_version(source):
    with open(source, encoding='utf-8') as f:
        lines = f.readlines()

    for l in lines:
        if 'pragma' in l and not 'experimental' in l:
            conditions = list(map(
                lambda v: OperatorVersionTuple(
                    ops[v[1]], SolidityVersion(v[2])),
                comp_version_rex.findall(l))
            )

            def fullfills_all_conditions(v):
                return all(map(lambda cond: cond.op(v, cond.v), conditions))
            try:
                return min(filter(fullfills_all_conditions, list(get_supported_solc_versions())))
            except ValueError:
                raise CompilerVersionNotSupported()
    else:
        return list(get_supported_solc_versions())[-1]


def compile_solfiles(files, proj_dir, solc_version=None, output_values=OUTPUT_VALUES, remappings=None):
    def complete_remapping(remapping):
        name, old_path = remapping.split('=')
        new_path = os.path.join(proj_dir, old_path)
        return f'{name}={new_path}'

    if remappings is None:
        remappings = []
    remappings = [complete_remapping(remapping) for remapping in remappings]
    node_modules_dir = find_node_modules_dir(proj_dir)
    if node_modules_dir is not None:
        zeppelin_path = os.path.abspath(os.path.join(node_modules_dir, 'zeppelin-solidity'))
        open_zeppelin_path = os.path.abspath(os.path.join(node_modules_dir, 'openzeppelin-solidity'))
        if os.path.isdir(zeppelin_path):
            remappings.append(f'zeppelin-solidity={zeppelin_path}')
        if os.path.isdir(open_zeppelin_path):
            remappings.append(f'openzeppelin-solidity={open_zeppelin_path}')

    if solc_version is None:
        solc_version = max(map(parse_version, files))

    binary = _get_binary(solc_version)

    combined_json = ','.join(output_values)
    compiler_kwargs = {
        'import_remappings': remappings,
        'allow_paths': proj_dir,
        'source_files': files,
        'solc_binary': binary,
        'combined_json': combined_json
    }


    try:
        stdoutdata, _, _, _ = solc_wrapper(**compiler_kwargs)
        return _parse_compiler_output(stdoutdata)
    except SolcError as e:
        raise SolidityCompilationException(e, files)


def compile_project(path, remappings=None):
    sources = get_sol_files(path)
    if not sources:
        raise NoSolidityProject(path)
    return compile_solfiles(sources, path)


def get_sol_files(src_dir_path):
    return [os.path.join(p, f) for p, _, fs in os.walk(src_dir_path) for f in fs if
            f.endswith('.sol') and
            'node_modules' not in p and
            '/test/' not in p[len(src_dir_path):] and
            not p.endswith('/test')]


def install_all_versions():
    # First install last version
    if not install_last_version():
        print("Failed to install all compiler. Trying to continue...")
        print("This might lead to later errors.")
        return False

    last_version = SolidityVersion(get_installed_solc_versions()[-1][1:])

    next_version = MINIMAL_SOLC_VERSION

    while SolidityVersion(next_version) < last_version:
        try:
            install_solc(f'v{next_version}')
            # Increase major version
            new_minor = int(next_version.split(".")[2]) + 1
            old_major = int(next_version.split(".")[1])
            next_version = f'0.{old_major}.{new_minor}'

        except (requests.exceptions.ConnectionError, subprocess.CalledProcessError) as e:
            # Increase major version
            new_major = int(next_version.split(".")[1]) + 1
            next_version = f'0.{new_major}.0'


def install_last_version():
    try:
        install_solc()
        return True
    except (requests.exceptions.ConnectionError, subprocess.CalledProcessError) as e:
        print("Failed to install latest compiler. Trying to continue...")
        print("This might lead to later errors.")
        return False


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit('Usage: %s PROJECT OUTPUT' % sys.argv[0])

    res = compile_project(sys.argv[1])

    if sys.argv[2] == '-':
        print(res)
    else:
        with open(sys.argv[2], 'w') as fs:
            json.dump(res, fs)
