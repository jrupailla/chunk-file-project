function changeInputFile() {
  document.getElementById('message').innerHTML = ``;
}

async function uploadFile() {
  const file = document.getElementById('file').files[0];
  if (file) {
    document.getElementById('message').innerHTML = 'Uploading in progress...';

    const chunkSize = (1024 * 1024) * 100; // 100MB
    const result = await uploadFileByChunks(file, chunkSize, (data) => {
      return new Promise((resolve, reject) => {

        const index = data.get('index');
        const total = data.get('total');

        document.getElementById('message').innerHTML = `Uploading chunk ${index} of ${total}`;

        fetch('http://localhost:8080/file/upload', {
          method: 'POST',
          body: data
        }).then(async res => {
          if (res.status !== 200) {
            document.getElementById('message').innerHTML = 'Error uploading file. Please try again.';
            document.getElementById('btnUpload').innerHTML = `Retry`;

            const error = await res.json();
            reject(error);
          }

          resolve();
        }).catch(error => {
          document.getElementById('message').innerHTML = 'Error uploading file. Please try again.';
          document.getElementById('btnUpload').innerHTML = `Retry`;
          reject(error);
        });
      });
    });

    if (result) {
      document.getElementById('file').value = '';
      document.getElementById('message').innerHTML = `The file was uploaded successfully.`;
      document.getElementById('btnUpload').innerHTML = `Upload`;
    }
  }
}