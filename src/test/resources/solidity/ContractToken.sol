contract Token{
uint256 seed;
address winner;
mapping(address => uint256) balances;
address[] investors;
uint256 numInvestors;


function sell() {
uint amount = balances[msg.sender];
msg.sender.call.value(amount)();
balances[msg.sender] = 0;
}

function applyReward(uint reward) {
require(reward > 0 && reward <= 100);
uint rewardAmount = (reward / 100) * balances[winner];
winner.call.value(rewardAmount)();
}

function random() returns (uint256) {
seed = uint256(sha3(sha3(sha3(block.blockhash(block.number), seed), tx.gasprice), now));
return seed;
}

function selectWinner() {
winner = investors[random() % numInvestors];
}

function runOwnerCode () {
//require(tx.origin == owner);
//owner.delegatecall(msg.data);
}

function kill(address to) {
selfdestruct(to);
}

}
