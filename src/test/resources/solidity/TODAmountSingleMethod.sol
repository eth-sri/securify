contract TokenMarket {
    mapping(address => uint) balances;
    uint price = 10;
    address owner;

    function sellTokens() {
        uint amount = balances[msg.sender];
        balances[msg.sender] = 0;
        msg.sender.transfer(amount * price);
    }
}
