document.addEventListener('DOMContentLoaded', function () {
    const searchSelect = document.getElementById('searchObject');

    // Handle change event for searchObject dropdown
    searchSelect.addEventListener('change', function () {
        updateSearchObject(this.value);
    });

    // Attach event listeners for file uploads
    const imageInput = document.getElementById('image');
    if (imageInput) {
        imageInput.addEventListener('change', function () {
            checkFiles(this.files);
        });
    }

    const videoInput = document.getElementById('video');
    if (videoInput) {
        videoInput.addEventListener('change', function () {
            checkVideo(this.files);
        });

    }
});
const socket = new WebSocket('ws://localhost:8081');

socket.onmessage = function (event) {
    console.log("WebSocket message received:", event.data);
    try {
        // Assuming you might expand to other JSON messages in the future
        const data = JSON.parse(event.data);
        if (data.command === 'update') {
            console.log("Update command received, updating image.");
            updateImage();
        }
    } catch (e) {
        // Fallback if the data is not JSON or not the expected JSON
        if (event.data === 'update') {
            console.log("Update command received, updating image.");
            updateImage();
        }
    }
};

socket.onerror = function (error) {
    console.error("WebSocket error observed:", error);
};

socket.onopen = function () {
    console.log("WebSocket connection established");
};

socket.onclose = function (event) {
    console.log("WebSocket is closed now.", event.reason);
};



function updateImage() {
    console.log('Attempting to update image...');
    const imgElement = document.getElementById('dynamicImage');
    if (imgElement) {

        imgElement.onload = function () {
            console.log("Image successfully loaded.");
        };
        imgElement.onerror = function () {
            console.error("Failed to load image.");
        };
        const imageUrl = 'http://localhost:3000/display.png';
        const timestamp = new Date().getTime(); // Cache busting
        const newSrc = `${imageUrl}?${timestamp}`;
        console.log('Updating image src to:', newSrc);
        imgElement.src = newSrc;
    } else {
        console.error('Element with ID "dynamicImage" was not found.');
    }
}





// Call updateImage at regular intervals
// Also call updateImage on page load
document.addEventListener('DOMContentLoaded', updateImage);

/*
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
*/


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
    }).then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.json();
    }).then(data => {
        if (data.detections && data.imagePath) {
            displayResults(data);
        } else {
            throw new Error('Missing necessary data');
        }
    }).catch(error => {
        console.error('Error:', error);
        document.getElementById('JSON_Display').innerHTML = 'Error processing the request: ' + error.message;
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
            if (data.detections && data.imagePath) {
                console.log('Detection Results:', data.detections);
                console.log('Image Path:', data.imagePath);
                displayResults(data); // Update this function to handle display updates correctly
            } else {
                console.error('Invalid data received', data);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            document.getElementById('JSON_Display').innerHTML = 'Error processing the request.';
        });


}


function displayResults(data) {
    console.log("Detection Results:", data.detections);
    console.log("Image Path:", data.imagePath);

    const resultsContainer = document.getElementById('JSON_Display');
    const imgElement = document.getElementById('resultImage');

    if (imgElement) {
        imgElement.src = `http://localhost:3000${data.imagePath}`;
        console.log("Image updated to:", imgElement.src);
    } else {
        console.error('Element with ID "resultImage" was not found.');
    }

    // Clear previous results
    resultsContainer.innerHTML = '';

    // Display detection results if any
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
}




function displayData(data) {
    const resultsDiv = document.getElementById('results');
    resultsDiv.innerHTML = ''; // Clear previous results

    // Create and append each key-value pair to the results div
    Object.keys(data).forEach(key => {
        const resultItem = document.createElement('div');
        resultItem.textContent = `${key}: ${data[key]}`;
        resultsDiv.appendChild(resultItem);
    });
}




