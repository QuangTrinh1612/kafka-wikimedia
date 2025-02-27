// JavaScript for Wikimedia Dashboard
// Connect to WebSocket server
let stompClient = null;
let wikisChart = null;
let typesChart = null;
let totalEdits = 0;
let editsInLastMinute = 0;
let maxEditsPerMinute = 10; // Initial assumption, will be adjusted
let uniqueUsers = new Set();
let lastMinuteTimestamp = Date.now();
let updateInterval;

// Modern color palette
const colorPalette = {
    primary: [
        '#4361ee', '#3a0ca3', '#4895ef', '#4cc9f0', '#3f37c9',
        '#7209b7', '#f72585', '#560bad', '#480ca8', '#b5179e'
    ],
    secondary: [
        '#ade8f4', '#90e0ef', '#48cae4', '#00b4d8', '#0096c7',
        '#0077b6', '#023e8a', '#03045e', '#0466c8', '#979dac'
    ]
};

// Initialize Bootstrap components
function initBootstrapComponents() {
    // Initialize tabs
    const triggerTabList = [].slice.call(document.querySelectorAll('#feedTabs .nav-link'));
    triggerTabList.forEach(function (triggerEl) {
        const tabTrigger = new bootstrap.Tab(triggerEl);
        triggerEl.addEventListener('click', function (event) {
            event.preventDefault();
            tabTrigger.show();
        });
    });
}

function connect() {
    const socket = new SockJS('/wikimedia-websocket');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logging
    
    stompClient.connect({}, function(frame) {
        console.log('Connected to WebSocket');
        
        // Subscribe to individual changes
        stompClient.subscribe('/topic/changes', function(message) {
            const change = JSON.parse(message.body);
            handleChange(change);
            updateLastUpdateTime();
        });
        
        // Subscribe to statistics
        stompClient.subscribe('/topic/statistics', function(message) {
            const stats = JSON.parse(message.body);
            updateStatistics(stats);
            updateLastUpdateTime();
        });

        // Start progress bar animations
        updateInterval = setInterval(animateProgressBars, 100);
    }, function(error) {
        console.log('STOMP error: ' + error);
        // Attempt to reconnect after a delay
        setTimeout(connect, 5000);
    });
}

function updateLastUpdateTime() {
    document.getElementById('update-time').textContent = 'Last update: ' + moment().format('HH:mm:ss');
}

function handleChange(change) {
    // Update live feed for all tabs
    addEventToFeed('live-feed-all', change);
    
    // Add to specific tab based on type
    if (change.type === 'new') {
        addEventToFeed('live-feed-new', change);
    } else if (change.type === 'edit') {
        addEventToFeed('live-feed-edit', change);
    }
    
    // Update counters
    totalEdits++;
    document.getElementById('total-edits').textContent = formatNumber(totalEdits);
    
    editsInLastMinute++;
    
    // Adjust max edits per minute if needed
    if (editsInLastMinute > maxEditsPerMinute) {
        maxEditsPerMinute = editsInLastMinute;
    }
    
    document.getElementById('edits-per-minute').textContent = formatNumber(editsInLastMinute);
    
    // Add user to unique users set
    uniqueUsers.add(change.user);
    document.getElementById('unique-users').textContent = formatNumber(uniqueUsers.size);
    
    // Reset the per-minute counter every minute
    const now = Date.now();
    if (now - lastMinuteTimestamp > 60000) {
        lastMinuteTimestamp = now;
        editsInLastMinute = 0;
    }
    
    // Update progress bars
    updateProgressBars();
}

function addEventToFeed(feedId, change) {
    const feedElement = document.getElementById(feedId);
    const eventElement = document.createElement('div');
    eventElement.classList.add('event-card');
    
    const timestamp = moment(change.timestamp).format('HH:mm:ss');
    const title = change.title;
    const user = change.user;
    const wiki = change.wiki;
    
    eventElement.innerHTML = `
        <div class="event-title">${title}</div>
        <div class="event-meta">
            <span class="badge-wiki">${wiki}</span>
            <span class="badge-user">@${user}</span>
            <span class="float-end text-muted small">${timestamp}</span>
        </div>
    `;
    
    feedElement.insertBefore(eventElement, feedElement.firstChild);
    
    // Keep only the last 50 events
    if (feedElement.children.length > 50) {
        feedElement.removeChild(feedElement.lastChild);
    }
    
    // Add fade-in animation
    setTimeout(() => {
        eventElement.style.opacity = '1';
    }, 10);
}

function updateStatistics(stats) {
    // Update total edits
    document.getElementById('total-edits').textContent = formatNumber(stats.totalEdits);
    totalEdits = stats.totalEdits;
    
    // Update wikis chart
    updateWikisChart(stats.editsByWiki);
    
    // Update types chart
    updateTypesChart(stats.editsByType);
    
    // Update progress bars
    updateProgressBars();
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
                    backgroundColor: colorPalette.primary,
                    borderColor: colorPalette.primary.map(color => adjustColor(color, -20)),
                    borderWidth: 1,
                    borderRadius: 4,
                    maxBarThickness: 40
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    },
                    tooltip: {
                        backgroundColor: 'rgba(255, 255, 255, 0.9)',
                        titleColor: '#212529',
                        bodyColor: '#212529',
                        borderColor: '#e9ecef',
                        borderWidth: 1,
                        cornerRadius: 8,
                        displayColors: true,
                        boxPadding: 6,
                        callbacks: {
                            label: function(context) {
                                return `Edits: ${context.raw}`;
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            display: true,
                            color: 'rgba(0, 0, 0, 0.05)',
                            drawBorder: false
                        },
                        ticks: {
                            font: {
                                size: 11
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        },
                        ticks: {
                            font: {
                                size: 11
                            }
                        }
                    }
                },
                animation: {
                    duration: 1000,
                    easing: 'easeOutQuart'
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
                    backgroundColor: colorPalette.primary,
                    borderColor: 'white',
                    borderWidth: 2,
                    hoverOffset: 10
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '65%',
                plugins: {
                    legend: {
                        position: 'right',
                        labels: {
                            boxWidth: 12,
                            padding: 15,
                            font: {
                                size: 11
                            }
                        }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(255, 255, 255, 0.9)',
                        titleColor: '#212529',
                        bodyColor: '#212529',
                        borderColor: '#e9ecef',
                        borderWidth: 1,
                        cornerRadius: 8,
                        displayColors: true,
                        boxPadding: 6
                    }
                },
                animation: {
                    duration: 1000,
                    easing: 'easeOutQuart'
                }
            }
        });
    }
}

function updateProgressBars() {
    // Update total edits progress based on some arbitrary scale for demonstration
    const totalEditsProgress = document.getElementById('total-edits-progress');
    totalEditsProgress.style.width = `${Math.min(totalEdits / 100, 100)}%`;
    
    // Update edits per minute progress
    const epmProgress = document.getElementById('epm-progress');
    epmProgress.style.width = `${(editsInLastMinute / maxEditsPerMinute) * 100}%`;
    
    // Update unique users progress based on some arbitrary scale
    const usersProgress = document.getElementById('users-progress');
    usersProgress.style.width = `${Math.min(uniqueUsers.size / 50, 100)}%`;
}

// Animate progress bars to give them a "pulsing" effect
function animateProgressBars() {
    const totalEditsProgress = document.getElementById('total-edits-progress');
    const epmProgress = document.getElementById('epm-progress');
    const usersProgress = document.getElementById('users-progress');
    
    const currentTime = Date.now();
    const pulseValue = Math.sin(currentTime / 500) * 0.1 + 0.9; // Value between 0.8 and 1.0
    
    totalEditsProgress.style.opacity = pulseValue;
    epmProgress.style.opacity = pulseValue;
    usersProgress.style.opacity = pulseValue;
}

// Helper function to format numbers with commas for thousands
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
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
    initBootstrapComponents();
});

// Cleanup when the page unloads
window.addEventListener('beforeunload', function() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    if (updateInterval) {
        clearInterval(updateInterval);
    }
});