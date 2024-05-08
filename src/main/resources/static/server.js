const express = require('express');
const WebSocket = require('ws');
const bodyParser = require('body-parser');
const multer = require('multer');
const app = express();
const PORT = 3000;
const path = require('path');

// Configure multer for file uploads
const storage = multer.diskStorage({
    destination: function(req, file, cb) {
        cb(null, path.join(__dirname, 'src/main/resources/static/predict_img'));
    },
    filename: function(req, file, cb) {
        cb(null, file.originalname);
    }
});
const upload = multer({ storage: storage });

app.use(bodyParser.json()); // Make sure to use bodyParser to parse JSON POST requests

// Serving static files from 'display' directory
app.use(express.static(path.join(__dirname, 'display')));

// Serving static files from 'predict_img' directory
app.use('/predict_img', express.static(path.join(__dirname, 'predict_img')));

app.use((req, res, next) => {
    console.log('Request URL:', req.originalUrl);
    next();
});

// WebSocket server on a different port
const wss = new WebSocket.Server({ port: 8081 });
wss.on('connection', function connection(ws) {
    ws.on('message', function incoming(message) {
        console.log('received: %s', message);
    });
});

// POST route to trigger updates to clients
app.post('/notify', (req, res) => {
    console.log("Received notification from Java backend");
    wss.clients.forEach(client => {
        if (client.readyState === WebSocket.OPEN) {
            client.send('update');
        }
    });
    res.status(200).send("Notification sent to all clients.");
});

// POST endpoint for image analysis
app.post('/analyze', upload.single('image'), (req, res) => {
    const file = req.file;
    if (!file) {
        return res.status(400).send('No file uploaded.');
    }

    // Dummy response for analysis result
    const result = {
        detections: [{ className: 'ExampleObject', probability: 0.95 }],
        imagePath: `/predict_img/${file.filename}` // Ensure the path is correct
    };

    res.json(result);
});

// Listen on HTTP port
app.listen(PORT, () => {
    console.log(`HTTP server running on port ${PORT}`);
});
