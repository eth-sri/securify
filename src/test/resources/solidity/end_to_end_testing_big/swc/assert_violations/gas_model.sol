/*
 * @source: ChainSecurity
 * @author: Anton Permenev
 */
pragma solidity ^0.5.0;

contract GasModel{
    uint x = 100;
    function check() public {
        uint a = gasleft();
        x = x + 1;
        uint b = gasleft();
        assert(b > a);
    }
}
