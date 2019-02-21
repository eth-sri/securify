pragma solidity ^0.5.0;
contract MarketPlace {
    function transfer() payable public {
        uint x = msg.value;
        msg.sender.send(0);
    }
	
}
