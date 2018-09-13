pragma solidity 0.4.24;
contract MarketPlace {
    function transfer() {
        uint x = msg.value;
        msg.sender.send(10);
    }
	
}