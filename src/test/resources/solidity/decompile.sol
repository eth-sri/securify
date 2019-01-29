contract A {
    uint a;
    function f1(uint i) {
        a = i;
    }
    function f2(uint i) {
        a = i;
    }

    function g(uint i) {
        var f = i < 100 ? f1 : f2;
        f(i);
    }

    function h() {
        msg.sender.transfer(a);
    }
}
