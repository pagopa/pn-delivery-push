const chunkIntoN = (arr, chunkSize, discardFun) => {

  if (chunkSize <= 0) 
    return {chunks: [], discarded: arr.length};

  if (discardFun == undefined || discardFun == null)
    discardFun = () => false;

  const chunks = [];
  let currChunkSize = 0;
  let currChunk = [];
  let discarded = 0;

  for (let i = 0; i < arr.length; i ++){

    if (discardFun(arr[i])) {
      discarded++;
      continue;
    }
      
    currChunkSize++;
    currChunk.push(arr[i]);
    if (currChunkSize === chunkSize) {
      chunks.push([...currChunk]);
      currChunkSize = 0;
      currChunk = [];
    }

  }

  if (currChunkSize !== 0) {
    chunks.push(currChunk);
  }

  return  {chunks, discarded};
};

module.exports = {
  chunkIntoN
};
