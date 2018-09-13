contract Wallet {

  uint balance;
  function send(){
    if (balance > 0){
      msg.sender.transfer(balance);
      balance = 0;
    }
  }
}
