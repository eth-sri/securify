pragma solidity ^0.4.24;

contract A {
  	address a;
	function f() public view returns(address) {
    	return a;
    }
}

contract B {
    mapping(address => bool) modified;
	A c = new A();
	function g(A a) public {
    	modified[address(a.f())] = true;
	}
}
