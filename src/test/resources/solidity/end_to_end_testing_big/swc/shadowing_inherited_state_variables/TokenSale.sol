pragma solidity ^0.5.0;

contract Tokensale {
    uint hardcap = 10000 ether;

    constructor() public {}

    function fetchCap() public view returns(uint) {
        return hardcap;
    }
}

contract Presale is Tokensale {
    uint hardcap = 1000 ether;

    constructor() Tokensale() public {}
}
