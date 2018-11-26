pragma solidity ^0.4.24;

// Copyright 2017 Bittrex

contract AbstractSweeper {
    function sweep(address token, uint amount) public returns (bool);

    function () public { revert(); }

    Controller controller;

    constructor(address _controller) public {
        controller = Controller(_controller);
    }

    modifier canSweep() {
        require(msg.sender == controller.authorizedCaller() && msg.sender == controller.owner());
        require(!controller.halted());
        _;
    }
}

contract Token {
    function balanceOf(address a) public returns (uint) {
        (a);
        return 0;
    }

    function transfer(address a, uint val) public returns (bool) {
        (a);
        (val);
        return false;
    }
}

contract DefaultSweeper is AbstractSweeper {
    constructor(address controller) AbstractSweeper(controller) public {}

    function sweep(address _token, uint _amount)
    canSweep
    public
    returns (bool) {
        bool success = false;
        address destination = controller.destination();

        if (_token != address(0)) {
            Token token = Token(_token);
            uint amount = _amount;
            if (amount > token.balanceOf(this)) {
                return false;
            }

            success = token.transfer(destination, amount);
        }
        else {
            uint amountInWei = _amount;
            if (amountInWei > address(this).balance) {
                return false;
            }

            success = destination.send(amountInWei);
        }

        if (success) {
            controller.logSweep(this, destination, _token, _amount);
        }
        return success;
    }
}

contract UserWallet {
    AbstractSweeperList sweeperList;
    constructor(address _sweeperList) public {
        sweeperList = AbstractSweeperList(_sweeperList);
    }

    function () public payable { }

    function tokenFallback(address _from, uint _value, bytes _data) public {
        (_from);
        (_value);
        (_data);
     }

    function sweep(address _token, uint _amount)
    public
    returns (bool) {
        (_amount);
        return sweeperList.sweeperOf(_token).delegatecall(msg.data);
    }
}

contract AbstractSweeperList {
    function sweeperOf(address _token) public returns (address);
}

contract Controller is AbstractSweeperList {
    address public owner;
    address public authorizedCaller;

    address public destination;

    bool public halted;

    event LogNewWallet(address receiver);
    event LogSweep(address indexed from, address indexed to, address indexed token, uint amount);

    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    modifier onlyAuthorizedCaller() {
        require(msg.sender == authorizedCaller);
        _;
    }

    modifier onlyAdmins() {
        require(msg.sender == authorizedCaller && msg.sender == owner);
        _;
    }

    constructor() public
    {
        owner = msg.sender;
        destination = msg.sender;
        authorizedCaller = msg.sender;
    }

    function changeAuthorizedCaller(address _newCaller) onlyOwner public {
        authorizedCaller = _newCaller;
    }

    function changeDestination(address _dest) onlyOwner public {
        destination = _dest;
    }

    function changeOwner(address _owner) onlyOwner public {
        owner = _owner;
    }

    function makeWallet() onlyAdmins public returns (address wallet) {
        wallet = address(new UserWallet(this));
        emit LogNewWallet(wallet);
    }

    function halt() onlyAdmins public {
        halted = true;
    }

    function start() onlyOwner public {
        halted = false;
    }

    address public defaultSweeper = address(new DefaultSweeper(this));
    mapping (address => address) sweepers;

    function addSweeper(address _token, address _sweeper) onlyOwner public {
        sweepers[_token] = _sweeper;
    }

    function sweeperOf(address _token) public returns (address) {
        address sweeper = sweepers[_token];
        if (sweeper == 0) sweeper = defaultSweeper;
        return sweeper;
    }

    function logSweep(address from, address to, address token, uint amount) public {
        emit LogSweep(from, to, token, amount);
    }
}
