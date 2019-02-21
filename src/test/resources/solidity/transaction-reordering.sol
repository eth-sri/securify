pragma solidity ^0.5.0;
contract MarketPlace {
    function safeTransfer1() payable public {
        uint x = msg.value;
        msg.sender.send(x);
    }
    
    function safeTransfer2() public {
        uint x = 100;
        uint y = x * 100;
        msg.sender.send(y);
    }
    
    function unsafeTransfer() public {
        uint x = address(this).balance;
        msg.sender.send(x);
    }
}
