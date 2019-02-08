contract A {
  	address a;
	function f() public view returns(address) {
    	return a;
    }
}

contract B {
	mapping(address => bool) approved;
	mapping(address => bool) modified;
	function g(A a) public {
		require(approved[(address(a.f()))]);
	}
	function h(A a) public {
		modified[(address(a.f()))] = true;
	}
}
