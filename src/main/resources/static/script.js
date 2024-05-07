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
            console.log(data); // Log the full data
            document.getElementById('JSON_Display').innerHTML = JSON.stringify(data, null, 2); // pretty print JSON

        }).catch(error => {
            console.error('Error:', error);

        });
}


/*
function checkVideo(files) {
    if (files.length === 1 && files[0].type === "video/mp4") {
        var videoFile = files[0];

        // Check if the video file size is greater than 200MB
        if (videoFile.size > 209715200) { // 200MB in bytes
            alert("The file is too large. Please upload a video file less than 200MB.");
            return;
        }


        var formData = new FormData();
        formData.append('video', videoFile);

        fetch('/upload_video', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                console.log('Video processing complete:', data);
                displayResults(data); // Function to display the results
            })
            .catch(error => console.error('Error uploading video:', error));
    } else {
        alert("Please upload exactly one MP4 video file.");
    }
}

function displayResults(data) {
    const answerYes = document.getElementById('answerYes');
    const answerNo = document.getElementById('answerNo');
    const JSON = document.getElementById('JSON');
    answerYes.innerHTML = '';
    answerNo.innerHTML = '';
    JSON.innerHTML = '';

    // Display results dynamically
    data.forEach(result => {
        const frameInfo = document.createElement('p');
        frameInfo.textContent = `Frame: ${result.frame}, Probability of 'Yes': ${result.probability.toFixed(2)}`;
        if (result.probability > 0.95) {
            answerYes.appendChild(frameInfo);
        } else {
            answerNo.appendChild(frameInfo);
        }
    });

    // Show the answer section
    document.getElementById('answerPart').style.visibility = 'visible';
}*/