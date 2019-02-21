/*
 * @source: https://github.com/ConsenSys/evm-analyzer-benchmark-suite
 * @author: Suhabe Bugrara
 */

pragma solidity ^0.5.0;

contract AssertConstructor {
    constructor() public {
        assert(false);
    }
}
