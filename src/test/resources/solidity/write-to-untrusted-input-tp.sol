contract A {
  	address a;
	function f() public view returns(address) {
    	return a;
    }
}

contract B {
    mapping(address => bool) modified;
    mapping(address => bool) approved;
	function g(A a) public {
    	modified[address(a.f())] = false;
    	approved[address(a.f())] = true;
	}
}
