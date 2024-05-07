function checkFiles(files) {
    console.log(files);

    if (files.length !== 1) {
        alert("Bitte genau eine Datei hochladen.");
        return;
    }

    const fileSize = files[0].size / 1024 / 1024; // in MiB
    if (fileSize > 10) {
        alert("Datei zu gross (max. 10Mb)");
        return;
    }

    answerPart.style.visibility = "visible";
    const file = files[0];

    // Preview
    if (file) {
        preview.src = URL.createObjectURL(file);
    }

    // Upload
    const formData = new FormData();
    formData.append("image", file);

    fetch('/analyze', {
        method: 'POST',
        body: formData
    }).then(response => response.json())
    .then(data => {
        displayResults(data, formData); // Function to display each JSON element separately
    })
    .catch(error => {
        console.error('Error:', error);
        document.getElementById('JSON_Display').innerHTML = 'Error processing the request.';
    });
    
    
}
function checkVideo(files) {
    console.log(files);

    if (files.length !== 1) {
        alert("Bitte genau eine Datei hochladen.");
        return;
    }

    const fileSize = files[0].size / 1024 / 1024; // in MiB
    if (fileSize > 200) {
        alert("Datei zu gross (max. 200Mb)");
        return;
    }

    answerPart.style.visibility = "visible";
    const file = files[0];

    // Preview
    /*
    if (file) {
        preview.src = URL.createObjectURL(file);
    }
    */

    // Upload
    const formData = new FormData();
    formData.append("video", file);

    fetch('/upload_video', {
        method: 'POST',
        body: formData
    }).then(response => response.json())
    .then(data => {
        displayResults(data, formData); // Function to display each JSON element separately
    })
    .catch(error => {
        console.error('Error:', error);
        document.getElementById('JSON_Display').innerHTML = 'Error processing the request.';
    });
    
    
}
function displayResults(data, formData) {
    const resultsContainer = document.getElementById('JSON_Display');
    const previeContainer = document.getElementById('preview');
    resultsContainer.innerHTML = ''; // Clear previous results
    previeContainer.innerHTML = formData

    data.forEach(element => {
        // Ensure both element and boundingBox exist
        if (element && element.boundingBox) {
            const elementDiv = document.createElement('div');
            elementDiv.className = 'result-item'; // Add a class for styling if needed
            elementDiv.innerHTML = `
                <p><strong>Class:</strong> ${element.className}</p>
                <p><strong>Probability:</strong> ${(element.probability * 100).toFixed(2)}%</p>
                
            `;

            //<p><strong>Bounds:</strong> Width: ${element.boundingBox.width.toFixed(3)}, Height: ${element.boundingBox.height.toFixed(3)}</p>
            resultsContainer.appendChild(elementDiv);
        } else {
            // Handle cases where the expected structure is not present
            console.log('Invalid element or missing boundingBox:', element);
            const errorDiv = document.createElement('div');
            errorDiv.className = 'error-item'; // Define this class in your CSS for error styling
            errorDiv.innerHTML = `<p>Error: Element is missing required data.</p>`;
            resultsContainer.appendChild(errorDiv);
        }

        getFrames();

        
    });
}

var stompClient = null;

function connect() {
    var socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/imageUpdate', function (imagePath) {
            updateImage(imagePath.body);
        });
    });
}

function getFrames() {
    fetch('/getFrames')
        .then(response => response.json())
        .then(files => {
            const container = document.getElementById('frameContainer');
            files.forEach(file => {
                const img = document.createElement('img');
                img.src = `/HighProb_Frames/${file}`; // Adjust path as necessary
                container.appendChild(img);
            });
        })
        .catch(error => console.error('Error loading the frames:', error));
};



