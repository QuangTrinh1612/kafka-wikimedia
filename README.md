# Project Overview
This Java application processes and visualizes the Wikimedia recent changes stream using Kafka. Here's how it works:

## Backend Components

- `WikimediaChangesProducer`: Connects to the Wikimedia stream API and publishes events to a Kafka topic.
- `WikimediaChangesHandler`: Processes the events from the EventSource API and sends them to Kafka.
- `WikimediaChangesConsumer`: Consumes events from Kafka, processes them, and sends updates to connected clients via WebSockets.
- `KafkaTopicConfig`: Configures the Kafka topic for the application.
- `WebSocketConfig`: Sets up WebSocket endpoints for real-time communication with the frontend.

## Frontend Components

- `HTML/CSS`: A responsive dashboard layout with cards for metrics, charts for visualization, and a live feed section.
- `JavaScript`:
    - Connects to the backend via WebSocket
    - Processes incoming data
    - Updates visualizations using Chart.js
    - Maintains real-time counters and statistics

## Features
- Bar chart showing top wikis by edit count
- Doughnut chart showing edit types distribution
![Dashboard Chart](src\main\resources\static\img\dashboard-landing-page.png)

- Live feed of recent changes
![Dashboard Live Feed](src\main\resources\static\img\dashboard-live-feed.png)

- Counters for total edits, edits per minute, and unique users
- Automatic reconnection if the WebSocket connection is lost

## How to Run
- Make sure you have Kafka running locally on port 9092
- Build the project using Maven: mvn clean install
- Run the application: java -jar target/wikimedia-stream-processing-1.0-SNAPSHOT.jar
- Open a browser and navigate to http://localhost:8080

The application will automatically connect to the Wikimedia stream, process the data through Kafka, and display visualizations in your browser.