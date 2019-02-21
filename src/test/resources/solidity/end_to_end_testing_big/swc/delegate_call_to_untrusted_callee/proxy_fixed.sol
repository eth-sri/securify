pragma solidity ^0.5.0;

contract Proxy {

  address callee;
  address owner;

  modifier onlyOwner {
    require(msg.sender == owner);
    _;
  }

  constructor() public {
  	callee = address(0x0);
    owner = msg.sender;
  }

  function setCallee(address newCallee) public onlyOwner {
  	callee = newCallee;
  }

  function forward(bytes memory _data) public {
    (bool success, bytes memory data) = (callee.delegatecall(_data));
    require(success);
  }

}
