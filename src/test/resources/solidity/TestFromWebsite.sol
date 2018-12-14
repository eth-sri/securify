library SafeMath {
function add(uint256 a, uint256 b) returns (uint256 c) {
c = a + b;
assert(c >= a);
return c;
}

function mul(uint256 a, uint256 b) internal pure returns (uint256 c) {
if (a == 0) {
return 0;
}
c = a * b;
assert(c / a == b);
return c;
}
}

contract Ownable {
using SafeMath for uint256;

address owner;

modifier onlyOwner() {
require(msg.sender == owner);
_;
}

function transferOwnership(address _owner) {
owner = _owner;
}
}

contract Wallet is Ownable {
address walletLibrary;

function () payable {
walletLibrary.delegatecall(msg.data);
}

function kill() {
selfdestruct(msg.sender);
}
}

contract Token is Ownable{
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

function applyReward(uint reward) onlyOwner {
require(reward > 0 && reward <= 100);
uint rewardAmount = (reward / 100) * balances[winner];
winner.call.value(rewardAmount)();
}

function random() returns (uint256) {
seed = uint256(sha3(sha3(sha3(block.blockhash(block.number), seed), tx.gasprice), now));
return seed;
}

function selectWinner() onlyOwner {
winner = investors[random() % numInvestors];
}

function runOwnerCode () {
require(tx.origin == owner);
owner.delegatecall(msg.data);
}

function kill(address to) {
selfdestruct(to);
}

}
