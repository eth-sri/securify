pragma solidity ^0.5.0;
contract MarketPlace {
    function someComp() payable public {
        uint x = msg.value;
    }
	
	function kill() public {
		selfdestruct(msg.sender);
	}  
}
