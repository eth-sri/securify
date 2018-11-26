pragma solidity ^0.4.24;

contract TimedCrowdsale {
  // Sale should finish exactly at January 1, 2019
  function isSaleFinished() view public returns (bool) {
    return block.timestamp >= 1546300800;
  }
}
