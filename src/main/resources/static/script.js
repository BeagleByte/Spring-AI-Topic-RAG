const API_BASE = `${window.location.protocol}//${window.location.host}/api/v1`;
let uploadedFiles = [];
let selectedTopic = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadTopics();
    loadStats();
    setupUploadListeners();
});

// Load topics
async function loadTopics() {
    try {
        const response = await fetch(`${API_BASE}/topics`);
        const topics = await response.json();

        const select = document.getElementById('topicSelect');
        Object.keys(topics).forEach(topic => {
            const option = document.createElement('option');
            option.value = topic;
            option.textContent = topic.charAt(0).toUpperCase() + topic.slice(1);
            select.appendChild(option);
        });

        select.addEventListener('change', (e) => {
            selectedTopic = e.target.value;
            if (selectedTopic) {
                const info = topics[selectedTopic];
                const infoDiv = document.getElementById('topicInfo');
                infoDiv.innerHTML = `<strong>${info.description}</strong>`;
                infoDiv.classList.add('show');
            }
        });
    } catch (error) {
        console.error('Error loading topics:', error);
    }
}

// Setup upload listeners
function setupUploadListeners() {
    setupUploadArea('pdf', '#pdfUploadArea', '#pdfInput', '#pdfStatus', uploadPdf);
    setupUploadArea('md', '#mdUploadArea', '#mdInput', '#mdStatus', uploadMarkdown);
}

function setupUploadArea(type, areaSelector, inputSelector, statusSelector, uploadHandler) {
    const area = document.querySelector(areaSelector);
    const input = document.querySelector(inputSelector);

    area.addEventListener('click', () => input.click());
    area.addEventListener('dragover', (e) => {
        e.preventDefault();
        e.stopPropagation();
    });
    area.addEventListener('drop', (e) => {
        e.preventDefault();
        e.stopPropagation();
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            uploadHandler(files);
        }
    });
    input.addEventListener('change', (e) => {
        if (e.target.files && e.target.files.length > 0) {
            uploadHandler(e.target.files);
        }
    });
}

async function uploadPdf(files) {
    if (!selectedTopic) {
        alert('Please select a topic first');
        return;
    }

    for (let file of files) {
        console.log('Uploading file:', file.name, 'Type:', file.type, 'Size:', file.size);

        const formData = new FormData();
        formData.append('file', file, file.name);  // Explicitly set filename

        const statusDiv = document.getElementById('pdfStatus');
        statusDiv.innerHTML = `<div class="status info">⏳ Uploading ${file.name}...</div>`;

        try {
            console.log('Sending request to:', `${API_BASE}/topics/${selectedTopic}/documents/upload/pdf`);

            const response = await fetch(
                `${API_BASE}/topics/${selectedTopic}/documents/upload/pdf`,
                {
                    method: 'POST',
                    body: formData
                    // CRITICAL: Do NOT set Content-Type header!
                    // Browser must set it automatically with multipart boundary
                }
            );

            console.log('Response status:', response.status);

            if (response.ok) {
                const data = await response.json();
                uploadedFiles.push(data);
                statusDiv.innerHTML = `<div class="status success">✅ ${file.name} - ${data.chunksCount} chunks indexed</div>`;
                updateFileList();
                loadStats();
            } else {
                const errorText = await response.text();
                console.error('Upload failed:', response.status, errorText);
                statusDiv.innerHTML = `<div class="status error">❌ Failed to upload ${file.name}: ${errorText}</div>`;
            }
        } catch (error) {
            console.error('Upload error:', error);
            statusDiv.innerHTML = `<div class="status error">❌ Error: ${error.message}</div>`;
        }
    }
}

async function uploadMarkdown(files) {
    if (!selectedTopic) {
        alert('Please select a topic first');
        return;
    }

    for (let file of files) {
        console.log('Uploading file:', file.name, 'Type:', file.type, 'Size:', file.size);

        const formData = new FormData();
        formData.append('file', file, file.name);  // Explicitly set filename

        const statusDiv = document.getElementById('mdStatus');
        statusDiv.innerHTML = `<div class="status info">⏳ Uploading ${file.name}...</div>`;

        try {
            console.log('Sending request to:', `${API_BASE}/topics/${selectedTopic}/documents/upload/markdown`);

            const response = await fetch(
                `${API_BASE}/topics/${selectedTopic}/documents/upload/markdown`,
                {
                    method: 'POST',
                    body: formData
                    // CRITICAL: Do NOT set Content-Type header!
                    // Browser must set it automatically with multipart boundary
                }
            );

            console.log('Response status:', response.status);

            if (response.ok) {
                const data = await response.json();
                uploadedFiles.push(data);
                statusDiv.innerHTML = `<div class="status success">✅ ${file.name} - ${data.chunksCount} chunks indexed</div>`;
                updateFileList();
                loadStats();
            } else {
                const errorText = await response.text();
                console.error('Upload failed:', response.status, errorText);
                statusDiv.innerHTML = `<div class="status error">❌ Failed to upload ${file.name}: ${errorText}</div>`;
            }
        } catch (error) {
            console.error('Upload error:', error);
            statusDiv.innerHTML = `<div class="status error">❌ Error: ${error.message}</div>`;
        }
    }
}

function updateFileList() {
    if (uploadedFiles.length > 0) {
        document.getElementById('fileList').style.display = 'block';
        document.getElementById('fileItems').innerHTML = uploadedFiles
            .map(f => `
                <div class="file-item">
                    <div class="info">
                        <strong>${f.filename}</strong>
                        <p>${f.chunksCount} chunks • ${f.type. toUpperCase()}</p>
                    </div>
                    <span class="badge">${f.status}</span>
                </div>
            `)
            .join('');
    }
}

async function submitQuery() {
    if (!selectedTopic) {
        alert('Please select a topic first');
        return;
    }

    const query = document.getElementById('queryInput').value.trim();
    if (!query) {
        alert('Please enter a question');
        return;
    }

    const loader = document.getElementById('loader');
    const resultDiv = document.getElementById('result');

    loader.classList.add('active');
    resultDiv.innerHTML = '';

    try {
        const response = await fetch(
            `${API_BASE}/topics/${selectedTopic}/query`,
            {
                method: 'POST',
                headers:  { 'Content-Type': 'application/json' },
                body: JSON.stringify({ query, topK: 5 })
            }
        );

        const data = await response.json();

        let sourcesHtml = `<strong>Sources (${data.sourceCount}):</strong>`;
        if (data.sources && data.sources.length > 0) {
            sourcesHtml += data.sources.map(s => `
                <div class="source-item">
                    <div class="title">${s.filename}</div>
                    <div class="meta">${s.title}${s.author ? ' • ' + s.author : ''}${s.publishingYear ? ' (' + s.publishingYear + ')' : ''}</div>
                </div>
            `).join('');
        }

        resultDiv.innerHTML = `
            <div class="result">
                <div class="answer">
                    <strong>Answer:</strong><br>
                    ${data. answer}
                </div>
                <div class="sources">
                    ${sourcesHtml}
                </div>
            </div>
        `;
    } catch (error) {
        resultDiv.innerHTML = `<div class="status error">Error: ${error.message}</div>`;
    } finally {
        loader.classList.remove('active');
    }
}

async function loadStats() {
    try {
        const response = await fetch(`${API_BASE}/topics/stats`);
        const stats = await response.json();

        const statsDiv = document. getElementById('stats');
        statsDiv.innerHTML = Object.entries(stats)
            .map(([topic, info]) => `
                <div class="stat-card">
                    <div class="stat-label">${topic.toUpperCase()}</div>
                    <div class="stat-value">${info.vectors_count || 0}</div>
                    <div class="stat-label">Vectors</div>
                </div>
            `)
            .join('');
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

document.getElementById('queryInput')?. addEventListener('keypress', (e) => {
    if (e. key === 'Enter') submitQuery();
});