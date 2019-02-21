contract SimpleBank {
    mapping(address => uint) balances;

    function withdraw(uint amount) public {
        balances[msg.sender] -= amount;
        msg.sender.transfer(amount);
    }
}
