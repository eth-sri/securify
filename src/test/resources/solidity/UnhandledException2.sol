pragma solidity ^0.5.0;

contract C{
    function f() public {
        if(uint(sha256(msg.data)) < 2){
            bool b = msg.sender.send(3);
            if(!b) {
                // throw;
            }
        }
    }
}
