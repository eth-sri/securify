pragma solidity 0.4.23;
contract A {
    address a;
    function f() public view returns(address) {
        return a;
    }
}

contract B {
    mapping(address => bool) approved;
    mapping(address => bool) modified;
    // Constant addresses are considered secure
    A constant a = A(0x123);

    function g() public {
        require(approved[(address(a.f()))]);
        modified[address(a.f())] = true;
    }
}
