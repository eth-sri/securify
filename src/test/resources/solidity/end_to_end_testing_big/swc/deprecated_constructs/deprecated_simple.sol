

contract DeprecatedSimple {
	
	function useDeprecated() public {

		// Do everything that's deprecated, then commit suicide.

		bytes32 blockhash = blockhash(0);
		bytes32 hashofhash = keccak256(abi.encodePacked(blockhash));

		uint gas = gasleft();

		if (gas == 0) {
			assert(false);
		}

		address(this).delegatecall("");

		selfdestruct(address(0));
	}

	function () external {}

}
