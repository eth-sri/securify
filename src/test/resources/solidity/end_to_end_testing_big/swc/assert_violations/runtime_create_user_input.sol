/*
 * @source: ChainSecurity
 * @author: Anton Permenev
 */
pragma solidity ^0.5.0;

contract RuntimeCreateUserInput{

    function check(uint x) public {
        B b = new B(x);
        assert(b.foo() == 10);
    }

}

contract B{

    uint x_;
    constructor(uint x) public {
        x_ = x;
    }

    function foo()  public returns(uint){
        return x_;
    }

}

