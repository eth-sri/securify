pragma solidity ^0.4.24;
contract MarketPlace {
    function someComp() {
        uint x = msg.value;
    }
	
	function kill() public {
		selfdestruct(msg.sender);
	}  
}
