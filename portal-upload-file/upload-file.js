let position = 0;

async function generateHashFile(file) {
  const chunkSize = (1024 * 1024) * 100; // 100MB
  const chunkCount = Math.ceil(file.size / chunkSize);
  const md = forge.md.sha256.create();

  for (let i = 0; i < chunkCount; i++) {
    const chunkStart = i * chunkSize;
    const chunkEnd = Math.min(file.size, (i + 1) * chunkSize);
    const fileSlice = file.slice(chunkStart, chunkEnd);
    const arrayBuffer = await fileSlice.arrayBuffer();
    const chunkBytes = new Uint8Array(arrayBuffer);

    const binaryChunks = [];
    const binaryChunkSize = 1024 * 10; // 10KB
    for (let i = 0; i < chunkBytes.length; i += binaryChunkSize) {
      binaryChunks.push(String.fromCharCode.apply(null, chunkBytes.subarray(i, i + binaryChunkSize)));
    }

    const binaryString = binaryChunks.join('');
    md.update(binaryString);
  }

  return md.digest().toHex();
}

async function uploadFileByChunks(file, chunkSize, callback) {
  if (position === 0) console.log('::: Uploading file :::', file.name);
  else console.log('::: Retry uploading file :::', file.name);
  
  const hashFile = await generateHashFile(file);
  const chunkCount = Math.ceil(file.size / chunkSize);
  console.log(`::: Chunked into ${chunkCount} parts :::`);

  while (position < file.size) {
    const chunkFile = file.slice(position, position + chunkSize);
    const chunkIndex = (position / chunkSize) + 1;

    const formData = new FormData();
    formData.append('file', chunkFile, file.name);
    formData.append('hash', hashFile);
    formData.append('index', chunkIndex);
    formData.append('total', chunkCount);

    try {
      console.log('::: Uploading chunk :::', chunkIndex);
      await callback(formData);
    } catch (error) {
      console.error('::: Error uploading chunk :::', error);
      return Promise.reject(false);
    }

    position += chunkSize;
  }

  position = 0;
  return Promise.resolve(true);
}