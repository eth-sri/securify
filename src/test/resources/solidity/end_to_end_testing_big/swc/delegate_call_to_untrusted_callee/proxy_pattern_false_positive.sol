pragma solidity ^0.5.0;

contract proxy{
  address owner;

  function proxyCall(address _to, bytes calldata _data) external {
    (bool success, bytes memory data) =  _to.delegatecall(_data);
    require(!success);
  }

  function withdraw() external{
    require(msg.sender == owner);
    msg.sender.transfer(address(this).balance);
  }
} 

/*
You can't use proxyCall to change the owner address as either: 

1) the delegatecall reverts and thus does not change owner
2) the delegatecall does not revert and therefore will cause the proxyCall to revert and preventing owner from changing

This false positive may seem like a really edge case, however since you can revert data back to proxy this patern is useful for proxy architectures
*/
