// MIT license
// 
// Copyright (C) 2018 Miguel Mota
// 
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do
// so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
// 
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE

// From https://github.com/miguelmota/solidity-create2-example
pragma experimental SMTChecker;
pragma solidity >0.4.99 <0.6.0;

contract Factory {
    event Deployed(address addr, uint256 salt);
    address addr;

    function deploy(bytes memory code, uint256 salt) public {
        address addrr;
        assembly {
            let addrr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addrr)) {
                revert(0, 0)
            }
        }

        addr = addrr;

        emit Deployed(addr, salt);
    }
}
