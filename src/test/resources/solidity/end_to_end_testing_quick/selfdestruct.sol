pragma solidity ^0.4.24;
contract C{
  function() public payable{
    selfdestruct(address(0x123));
  }
}
