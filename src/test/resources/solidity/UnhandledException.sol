pragma solidity ^0.5.0;

contract SimpleBank {
    mapping(address => uint) balances;

    function withdraw() public {
        msg.sender.send(balances[msg.sender]);
        balances[msg.sender] = 0;
    }
}
