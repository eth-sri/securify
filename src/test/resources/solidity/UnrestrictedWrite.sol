contract Ownable {
    address owner;

    function transferOwnership(address _newOwner) {
        owner = _newOwner;
    }
}
