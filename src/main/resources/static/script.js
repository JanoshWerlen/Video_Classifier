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
        displayResults(data, formData); // Function to display each JSON element separately
    })
    .catch(error => {
        console.error('Error:', error);
        document.getElementById('JSON_Display').innerHTML = 'Error processing the request.';
    });
    
    
}
function displayResults(data) {
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




