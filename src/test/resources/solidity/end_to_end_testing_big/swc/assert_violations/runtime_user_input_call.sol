/*
 * @source: ChainSecurity
 * @author: Anton Permenev
 */
pragma solidity ^0.5.0;

contract RuntimeUserInputCall{

    function check(address b) public {
        assert(B(b).foo() == 10);
    }

}

contract B{
    function foo()  public returns(uint);
}
