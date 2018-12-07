pragma solidity ^0.4.24;

contract TypoOneCommand {
    uint numberOne = 1;

    function alwaysOne() public {
        numberOne =+ 1;
    }
}
