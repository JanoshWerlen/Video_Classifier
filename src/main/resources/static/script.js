document.addEventListener('DOMContentLoaded', function () {
    const searchSelect = document.getElementById('searchObject');

    // Handle change event for searchObject dropdown
    searchSelect.addEventListener('change', function() {
        updateSearchObject(this.value);
    });

    // Attach event listeners for file uploads
    const imageInput = document.getElementById('image');
    if (imageInput) {
        imageInput.addEventListener('change', function() {
            checkFiles(this.files);
        });
    }

    const videoInput = document.getElementById('video');
    if (videoInput) {
        videoInput.addEventListener('change', function() {
            checkVideo(this.files);
        });
    }
});

const socket = new WebSocket('ws://localhost:8000');

socket.onmessage = function(event) {
    console.log("WebSocket message received:", event.data);
    if (event.data === 'update') {
        updateImage();
    }
};

function updateImage() {
    const imgElement = document.getElementById('dynamicImage');
    if (imgElement) {
        const imageUrl = 'http://localhost:3000/display.png';
        const timestamp = new Date().getTime(); // Cache busting
        const newSrc = `${imageUrl}?${timestamp}`;
        console.log('Updating image src to:', newSrc);  // Debugging log
        imgElement.src = newSrc;
    } else {
        console.error('Element with ID "dynamicImage" was not found.');
    }
}



// Call updateImage at regular intervals
// Also call updateImage on page load
document.addEventListener('DOMContentLoaded', updateImage);


function updateSearchObject(searchObject) {
    const formData = new FormData();
    formData.append('searchObject', searchObject);
    formData.append('_csrf', getCsrfToken()); // Append CSRF token if needed

    fetch('/setSearchObject', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text()) // Assuming the server responds with text.
    .then(data => {
        console.log('Success:', data);
        //alert('Classification set to: ' + searchObject);
    })
    .catch(error => {
        console.error('Error:', error);
    });
}

function getCsrfToken() {
    const tokens = document.getElementsByName('_csrf');
    if (tokens.length > 0) {
        return tokens[0].value;
    }
    return null;
}



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

    //answerPart.style.visibility = "visible";
    const file = files[0];

    const formData = new FormData();
    formData.append("image", file);

    fetch('/analyze', {
        method: 'POST',
        body: formData
    }).then(response => response.json())
    .then(data => {
        console.log('Detection Results:', data.detections);
        console.log('Image Path:', data.imagePath);
        displayResults(data); // Function to display each JSON element separately
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

    //answerPart.style.visibility = "visible";
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
        console.log(data)
        displayData(data); // Function to display each JSON element separately
    })
    .catch(error => {
        console.error('Error:', error);
        document.getElementById('JSON_Display').innerHTML = 'Error processing the request.';
    });
    
    
}
function displayResults(data) {

    console.log("Logged data: " + data.imagePath)
    const resultsContainer = document.getElementById('JSON_Display');
    const imageContainer = document.getElementById('frameContainer');
    resultsContainer.innerHTML = ''; // Clear previous results

    if (data.detections) {
        data.detections.forEach(detection => {
            const elementDiv = document.createElement('div');
            elementDiv.className = 'result-item';
            elementDiv.innerHTML = `
                <p><strong>Class:</strong> ${detection.className}</p>
                <p><strong>Probability:</strong> ${(detection.probability * 100).toFixed(2)}%</p>
            `;
            resultsContainer.appendChild(elementDiv);
        });
    }

    if (data.imagePath) {
        imageContainer.innerHTML = ''; // Clear previous image if any
        const img = document.createElement('img');
        img.src = data.imagePath; // Use the corrected path
        img.style.maxWidth = '100%';
        img.style.maxHeight = '100%';
        imageContainer.appendChild(img);
    }
}

function displayData(data) {
    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = ''; // Clear previous results

    // Create and append each key-value pair to the results div
    Object.keys(data).forEach(key => {
        const resultItem = document.createElement('div');
        resultItem.textContent = `${key}: ${data[key]}`;
        resultsDiv.appendChild(resultItem);
    });}




