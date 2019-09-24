pragma solidity ^0.5.0;

contract A {
    uint a;
    function f1(uint i) public {
        a = i;
    }
    function f2(uint i) public {
        a = i;
    }

    function g(uint i) public {
        function (uint256) f = i < 100 ? f1 : f2;
        f(i);
    }

    function h() public {
        msg.sender.transfer(a);
    }
}
