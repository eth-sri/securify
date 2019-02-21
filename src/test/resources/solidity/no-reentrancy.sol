pragma solidity ^0.5.0;
contract MarketPlace {
    uint balance = 0;
    
    function noReentrancy() public {
        uint x = balance;
        balance = 0;
        msg.sender.call.value(x)("");
    }
    
    // function reentrancy() {
    //     uint x = balance;
    //     msg.sender.call.value(x)();
    //     balance = 0;
    // }
    
}

