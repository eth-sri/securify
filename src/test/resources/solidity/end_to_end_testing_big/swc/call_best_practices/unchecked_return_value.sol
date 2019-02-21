pragma solidity ^0.5.0;

contract ReturnValue {

  address callee = 0xDeaDbeefdEAdbeefdEadbEEFdeadbeEFdEaDbeeF;

  function callchecked() public {
  	(bool success, bytes memory data) = (callee.call(""));
        require(success);
  }

  function callnotchecked() public {
    callee.call("");
  }
}
