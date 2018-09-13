contract Game {
    address winner;

    function play(bytes guess) {
        if (keccak256(guess) == 0xDEADBEEF) {
            winner = msg.sender;
        }
    }
    function getReward() {
        winner.transfer(msg.value);
    }
}
