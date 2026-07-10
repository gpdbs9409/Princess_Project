async function api(url, method = "GET", body) {
    const token = localStorage.getItem("princess_token");
    const headers = {};
    if (body) headers["Content-Type"] = "application/json";
    if (token) headers["Authorization"] = "Bearer " + token;

    const response = await fetch(url, {
        method,
        headers,
        body: body ? JSON.stringify(body) : undefined,
    });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`${method} ${url} failed: ${response.status} ${text}`);
    }
    if (response.status === 204) return null;
    return response.json();
}

async function uploadFile(file) {
    const token = localStorage.getItem("princess_token");
    const headers = {};
    if (token) headers["Authorization"] = "Bearer " + token;

    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/uploads", { method: "POST", headers, body: formData });
    if (!response.ok) {
        const text = await response.text();
        throw new Error(`upload failed: ${response.status} ${text}`);
    }
    return response.json();
}

function todayIso() {
    return new Date().toISOString().slice(0, 10);
}
