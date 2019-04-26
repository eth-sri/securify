pragma solidity 0.4.23;
contract Ownable {
    address owner;

    function transferOwnership(address _newOwner) public {
        owner = _newOwner;
    }
}
