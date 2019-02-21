/*
 * @source: https://consensys.github.io/smart-contract-best-practices/recommendations/#avoid-using-txorigin
 * @author: Consensys Diligence
 * Modified by Gerhard Wagner
 */

pragma solidity ^0.5.0;

contract MyContract {

    address owner;

    constructor() public {
        owner = msg.sender;
    }

    function sendTo(address payable receiver, uint amount) public {
      require(msg.sender == owner);
      receiver.transfer(amount);
    }

}
