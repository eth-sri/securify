contract Ownable {
    address owner;

    function transferOwnership(address _newOwner) public {
        owner = _newOwner;
    }
}
