pragma solidity 0.5.4;
contract Game {
    address payable winner;

    function play(bytes memory guess) public {
        if (uint(keccak256(guess)) == 0xDEADBEEF) {
            winner = msg.sender;
        }
    }
    function getReward() payable public {
        winner.transfer(msg.value);
    }
}
