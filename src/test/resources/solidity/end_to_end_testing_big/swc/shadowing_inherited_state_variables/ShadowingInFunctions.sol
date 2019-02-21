pragma solidity ^0.5.0;

contract ShadowingInFunctions {
    uint n = 2;
    uint x = 3;

    function test1() public view returns (uint n) {
        return n; // Will return 0
    }

    function test2() public view returns (uint n) {
        n = 1;
        return n; // Will return 1
    }

    function test3() public view returns (uint x) {
        uint n = 4;
        return n+x; // Will return 4
    }
}
