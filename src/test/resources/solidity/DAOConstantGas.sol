pragma solidity 0.4.23;
contract Wallet {

  uint balance;
  function send() public {
    if (balance > 0){
      msg.sender.transfer(balance);
      balance = 0;
    }
  }
}
