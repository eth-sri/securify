contract Game {
    bool won = false;

    function play(bytes guess) {
        require(!won);
        if (keccak256(guess) == 0xDEADBEEF) {
            won = true;
            msg.sender.transfer(10 ** 18);
        }
    }
}
