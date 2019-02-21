pragma solidity ^0.5.0;
contract C{
  function() external payable{
    selfdestruct(address(0x123));
  }
}
