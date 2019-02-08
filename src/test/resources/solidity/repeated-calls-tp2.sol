contract A {
  	address a;
	function f() public view returns(address) {
    	return a;
    }
}

contract B {
	mapping(address => bool) approved;
    mapping(address => bool) modified;

    function x(A a) internal returns(address){
        return address(a.f());
    }

	function g(A a) public {
		require(approved[x(a)]);
    	modified[x(a)] = true;
	}
}
