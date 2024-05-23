let position = 0;

async function generateHashFile(file) {
  const arrayBuffer = await file.arrayBuffer();
  const hashBuffer = await crypto.subtle.digest('SHA-256', arrayBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  return hashHex;
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