pragma solidity ^0.4.24;

contract Wallet {
  address owner;
  bool withdrawable = true;

  function withdraw() public {
    require(withdrawable);
    withdrawable = false;
    owner.transfer(1);
  }

  function () payable external {}
}
