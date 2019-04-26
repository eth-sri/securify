pragma solidity 0.4.23;
contract Wallet {

  uint balance;
  function send(){
    if (balance > 0){
      msg.sender.call.value(balance)();
      balance = 0;
    }
  }
}
