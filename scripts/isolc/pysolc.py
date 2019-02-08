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
from pathlib import Path
import sys
import json

from solcx import install_solc
from solcx import get_solc_folder
from solcx.main import _parse_compiler_output
from solcx.wrapper import solc_wrapper
from solcx.exceptions import SolcError
import solcx.install


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


OUTPUT_VALUES = ('abi',
                 'ast',
                 'bin-runtime',
                 'srcmap-runtime')

comp_version1_rex = re.compile(r'0\.\d+\.\d+')


def _version_to_tuple(v):
    return tuple(map(int, v.split('.')))

SOLC_VERSIONS = []
for i in range(11, 26):
	SOLC_VERSIONS.append(f'0.4.{i}')
for i in range(4):
	SOLC_VERSIONS.append(f'0.5.{i}')
DEFAULT_SOLC_VERSION = SOLC_VERSIONS[-1]


def parse_version(source):
    with open(source, encoding='utf-8') as f:
        for l in f.readlines():
            if 'pragma' in l and not 'experimental' in l:
                if '^' in l or '>' in l:
                    return DEFAULT_SOLC_VERSION
                else:
                    solc_version = comp_version1_rex.findall(l)[0]
                    if solc_version not in SOLC_VERSIONS:
                        raise CompilerVersionNotSupported(
                            solc_version, solc_version < SOLC_VERSIONS[0])
                    return solc_version
    return DEFAULT_SOLC_VERSION


def find_node_modules_dir(contracts):
    for x in os.walk(contracts):
        if os.path.isdir(os.path.join(x[0], 'node_modules')):
            return os.path.join(x[0], 'node_modules')
    return None


def compile_solfiles(files, proj_dir, solc_version=None, output_values=OUTPUT_VALUES):
    remappings = []
    node_modules_dir = find_node_modules_dir(proj_dir)

    if node_modules_dir is not None:
        zeppelin_path = os.path.abspath(os.path.join(
            node_modules_dir, 'zeppelin-solidity'))
        open_zeppelin_path = os.path.abspath(
            os.path.join(node_modules_dir, 'openzeppelin-solidity'))
        if os.path.isdir(zeppelin_path):
            remappings.append(f'zeppelin-solidity={zeppelin_path}')
        if os.path.isdir(open_zeppelin_path):
            remappings.append(f'openzeppelin-solidity={open_zeppelin_path}')

    if solc_version is None:
        solc_version = min(map(parse_version, files),
                           key=_version_to_tuple)

    binary = os.path.join(get_solc_folder(), f'solc-v{solc_version}')

    combined_json = ','.join(output_values)
    compiler_kwargs = {'import_remappings': remappings,
                       'allow_paths': proj_dir,
                       'source_files': files,
                       'solc_binary': binary,
                       'combined_json': combined_json}

    try:
        stdoutdata, _, _, _ = solc_wrapper(**compiler_kwargs)
        return _parse_compiler_output(stdoutdata)
    except SolcError as e:
        raise SolidityCompilationException(e, files)


def compile_project(path):
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
    for v in SOLC_VERSIONS:
        install_solc(f'v{v}')


def install_last_version():
    v = SOLC_VERSIONS[-1]
    install_solc(f'v{v}')


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit('Usage: %s PROJECT OUTPUT' % sys.argv[0])

    res = compile_project(sys.argv[1])

    if sys.argv[2] == '-':
        print(res)
    else:
        with open(sys.argv[2], 'w') as fs:
            json.dump(res, fs)
