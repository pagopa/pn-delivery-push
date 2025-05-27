const { expect } = require("chai");
const sinon = require("sinon");
const { describe, it } = require("mocha");

const {
chunkIntoN, isLambdaDisabled
} = require("../app/utils");

let oddTestArray = [1,2,3,4,5,6,7,10,9,10,11];
let evenTestArray = [1,2,3,4,5,6,7,10,9,10];
let onlyEvenTestArray = [12,22,32,42,52,62,72,10,92,100];

let noDiscard = () => false;
describe("Test Utils Functions", () => {
  it("chunking odd: all elems", () => {
    const res = chunkIntoN(oddTestArray, 2, noDiscard);
    expect(res.discarded).to.be.equal(0);
    expect(res.chunks.length).to.be.equal(6);
  });

  it("chunking odd: wrong chunk size", () => {
    const res = chunkIntoN(oddTestArray, -2, noDiscard);
    expect(res.discarded).to.be.equal(oddTestArray.length);
    expect(res.chunks.length).to.be.equal(0);
  });

  it("chunking odd: no provided fun", () => {
    const res = chunkIntoN(oddTestArray, 2);
    expect(res.discarded).to.be.equal(0);
    expect(res.chunks.length).to.be.equal(6);
  });


  it("chunking even: all elems", () => {
    const res = chunkIntoN(evenTestArray, 2, noDiscard);
    expect(res.discarded).to.be.equal(0);
    expect(res.chunks.length).to.be.equal(5);
  });
  
  it("chunking even: all elems", () => {
    const discardEvenFun =  (x) => ((x % 2) == 0); 
    const res = chunkIntoN(evenTestArray, 2, discardEvenFun );
    expect(res.discarded).to.be.equal(5);
    expect(res.chunks.length).to.be.equal(3);
  });

  it("chunking even: discard all elems", () => {
    const discardEvenFun =  (x) => ((x % 2) == 0); 
    const res = chunkIntoN(onlyEvenTestArray, 2, discardEvenFun );
    expect(res.discarded).to.be.equal(10);
    expect(res.chunks.length).to.be.equal(0);
  });


  it("chunking even: chunk size grater than array", () => {
    const res = chunkIntoN(onlyEvenTestArray, 12, noDiscard );
    expect(res.discarded).to.be.equal(0);
    expect(res.chunks.length).to.be.equal(1);
  });
});

describe("isLambdaDisabled", function() {
  let clock;

  afterEach(() => {
    if (clock) clock.restore();
  });

  it("should return true if currentDate is before start", () => {
    // Set current date to 2023-01-01T00:00:00.000Z
    clock = sinon.useFakeTimers(new Date("2023-01-01T00:00:00.000Z").getTime());
    const featureFlag = {
      start: "2023-02-01T00:00:00.000Z",
      end: "2023-03-01T00:00:00.000Z"
    };
    expect(isLambdaDisabled(featureFlag)).to.be.true;
  });

  it("should return true if currentDate is after end", () => {
    // Set current date to 2023-04-01T00:00:00.000Z
    clock = sinon.useFakeTimers(new Date("2023-04-01T00:00:00.000Z").getTime());
    const featureFlag = {
      start: "2023-02-01T00:00:00.000Z",
      end: "2023-03-01T00:00:00.000Z"
    };
    expect(isLambdaDisabled(featureFlag)).to.be.true;
  });

  it("should return false if currentDate is between start and end", () => {
    // Set current date to 2023-02-15T12:00:00.000Z
    clock = sinon.useFakeTimers(new Date("2023-02-15T12:00:00.000Z").getTime());
    const featureFlag = {
      start: "2023-02-01T00:00:00.000Z",
      end: "2023-03-01T00:00:00.000Z"
    };
    expect(isLambdaDisabled(featureFlag)).to.be.false;
  });
});
