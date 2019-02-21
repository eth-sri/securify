pragma solidity ^0.5.0;

contract Wallet {
  address payable owner;
  bool withdrawable = true;

  function withdraw() public {
    require(withdrawable);
    withdrawable = false;
    owner.transfer(1);
  }

  function () payable external {}
}
