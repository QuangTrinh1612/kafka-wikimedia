// Connect to WebSocket server
let stompClient = null;
let wikisChart = null;
let typesChart = null;
let totalEdits = 0;
let editsInLastMinute = 0;
let uniqueUsers = new Set();
let lastMinuteTimestamp = Date.now();

// Colors for charts
const colors = [
    '#4e73df', '#1cc88a', '#36b9cc', '#f6c23e', '#e74a3b',
    '#5a5c69', '#858796', '#f8f9fc', '#d1d3e2', '#2e59d9'
];

function connect() {
    const socket = new SockJS('/wikimedia-websocket');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        
        // Subscribe to individual changes
        stompClient.subscribe('/topic/changes', function(message) {
            const change = JSON.parse(message.body);
            handleChange(change);
        });
        
        // Subscribe to statistics
        stompClient.subscribe('/topic/statistics', function(message) {
            const stats = JSON.parse(message.body);
            updateStatistics(stats);
        });
    }, function(error) {
        console.log('STOMP error: ' + error);
        // Attempt to reconnect after a delay
        setTimeout(connect, 5000);
    });
}

function handleChange(change) {
    // Update live feed
    const feedElement = document.getElementById('live-feed');
    const eventElement = document.createElement('div');
    eventElement.classList.add('event-card');
    
    const timestamp = new Date(change.timestamp).toLocaleTimeString();
    const title = change.title;
    const user = change.user;
    const wiki = change.wiki;
    
    eventElement.innerHTML = `
        <div><strong>${timestamp}</strong> - ${title}</div>
        <div>User: ${user} | Wiki: ${wiki}</div>
    `;
    
    feedElement.insertBefore(eventElement, feedElement.firstChild);
    
    // Keep only the last 50 events
    if (feedElement.children.length > 50) {
        feedElement.removeChild(feedElement.lastChild);
    }
    
    // Update counters
    totalEdits++;
    document.getElementById('total-edits').textContent = totalEdits;
    
    editsInLastMinute++;
    uniqueUsers.add(user);
    document.getElementById('unique-users').textContent = uniqueUsers.size;
    
    // Reset the per-minute counter every minute
    const now = Date.now();
    if (now - lastMinuteTimestamp > 60000) {
        document.getElementById('edits-per-minute').textContent = editsInLastMinute;
        editsInLastMinute = 0;
        lastMinuteTimestamp = now;
    }
}

function updateStatistics(stats) {
    // Update total edits
    document.getElementById('total-edits').textContent = stats.totalEdits;
    
    // Update wikis chart
    updateWikisChart(stats.editsByWiki);
    
    // Update types chart
    updateTypesChart(stats.editsByType);
}

function updateWikisChart(wikisData) {
    const labels = Object.keys(wikisData);
    const data = Object.values(wikisData);
    
    if (wikisChart) {
        wikisChart.data.labels = labels;
        wikisChart.data.datasets[0].data = data;
        wikisChart.update();
    } else {
        const ctx = document.getElementById('wikis-chart').getContext('2d');
        wikisChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Edits by Wiki',
                    data: data,
                    backgroundColor: colors,
                    borderColor: colors.map(color => adjustColor(color, -20)),
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
}

function updateTypesChart(typesData) {
    const labels = Object.keys(typesData);
    const data = Object.values(typesData);
    
    if (typesChart) {
        typesChart.data.labels = labels;
        typesChart.data.datasets[0].data = data;
        typesChart.update();
    } else {
        const ctx = document.getElementById('types-chart').getContext('2d');
        typesChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderColor: colors.map(color => adjustColor(color, -20)),
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        });
    }
}

// Helper function to adjust color brightness
function adjustColor(color, amount) {
    const colorObj = hexToRgb(color);
    if (!colorObj) return color;
    
    const { r, g, b } = colorObj;
    const newR = Math.max(0, Math.min(255, r + amount));
    const newG = Math.max(0, Math.min(255, g + amount));
    const newB = Math.max(0, Math.min(255, b + amount));
    
    return rgbToHex(newR, newG, newB);
}

function hexToRgb(hex) {
    // Remove # if present
    hex = hex.replace(/^#/, '');
    
    // Parse hex
    const bigint = parseInt(hex, 16);
    const r = (bigint >> 16) & 255;
    const g = (bigint >> 8) & 255;
    const b = bigint & 255;
    
    return { r, g, b };
}

function rgbToHex(r, g, b) {
    return '#' + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
}

// Connect when the page loads
document.addEventListener('DOMContentLoaded', function() {
    connect();
});