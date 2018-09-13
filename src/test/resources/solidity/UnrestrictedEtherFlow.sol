contract Wallet {
    address owner;

    function withdraw() {
        owner.transfer(msg.value);
    }

    function () payable {}
}