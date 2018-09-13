pragma solidity 0.4.24;

contract game {
  bool won = false;

  function play() public {
    require(!won);
    won = true;
    msg.sender.transfer(10 ** 18);
  }
}
