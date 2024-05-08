const express = require('express');
const WebSocket = require('ws');
const bodyParser = require('body-parser');
const app = express();
const PORT = 3000;
const path = require('path');

app.use(bodyParser.json()); // Make sure to use bodyParser to parse JSON POST requests
app.use((req, res, next) => {
    console.log('Request URL:', req.originalUrl);
    next();
});

// Serve static files directly from the current directory + "/display"
app.use(express.static(path.join(__dirname, 'display')));
console.log(path.join(__dirname, 'src/main/resources/static/display'));

// WebSocket server on a different port
const wss = new WebSocket.Server({ port: 8000 });
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



// Listen on HTTP port
app.listen(PORT, () => {
    console.log(`HTTP server running on port ${PORT}`);
});
