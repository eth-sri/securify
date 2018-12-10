pragma solidity ^0.4.24;
contract MarketPlace {
    function safeTransfer1() {
        uint x = msg.value;
        msg.sender.send(x);
    }
    
    function safeTransfer2() {
        uint x = 100;
        uint y = x * 100;
        msg.sender.send(y);
    }
    
    function unsafeTransfer() {
        uint x = this.balance;
        msg.sender.send(x);
    }
}
