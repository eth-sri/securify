pragma solidity ^0.5.0;

contract TestStorage {

    uint storeduint1 = 15;
    uint constant constuint = 16;
    uint32 investmentsDeadlineTimeStamp = uint32(now); 

    bytes16 string1 = "test1"; 
    bytes32 private string2 = "test1236"; 
    string public string3 = "lets string something"; 

    mapping (address => uint) public uints1; 
    mapping (address => DeviceData) structs1; 

    uint[] uintarray; 
    DeviceData[] deviceDataArray; 

    struct DeviceData {
        string deviceBrand;
        string deviceYear;
        string batteryWearLevel;
    }

    function testStorage() public  {
        address address1 = 0xbCcc714d56bc0da0fd33d96d2a87b680dD6D0DF6;
        address address2 = 0xaee905FdD3ED851e48d22059575b9F4245A82B04;

        uints1[address1] = 88;
        uints1[address2] = 99;

        DeviceData memory dev1 = DeviceData("deviceBrand", "deviceYear", "wearLevel");

        structs1[address1] = dev1;

        uintarray.push(8000);
        uintarray.push(9000);

        deviceDataArray.push(dev1);
    }
}
