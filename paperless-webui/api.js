const API_BASE = "http://localhost:8080/api";
const NETWORK_ERROR = "Server not reachable. Please try again later.";

function getToken() {
  return localStorage.getItem("token");
}

function setToken(token) {
  localStorage.setItem("token", token);
}

function clearToken() {
  localStorage.removeItem("token");
}

function isLoggedIn() {
  return !!getToken();
}

async function apiFetch(path, options = {}) {
  const headers = options.headers || {};
  const token = getToken();
  if (token) {
    headers["Authorization"] = "Bearer " + token;
  }
  const res = await fetch(API_BASE + path, { ...options, headers });
  if (res.status === 401) {
    clearToken();
    window.location.href = "index.html";
    throw new Error("Not authenticated");
  }
  return res;
}

function setLoading(btn, loading) {
  btn.disabled = loading;
  btn.classList.toggle("loading", loading);
}

function showError(message) {
  let box = document.getElementById("errorBox");
  if (!box) {
    box = document.createElement("p");
    box.id = "errorBox";
    box.setAttribute("role", "alert");
    box.style.cssText = "color:#b3261e;background:#fbe3e1;padding:8px 12px;border-radius:6px;margin:8px 0";
    const main = document.querySelector("main");
    main.insertBefore(box, main.children[1] || null);
  }
  box.textContent = message;
  box.hidden = false;
}

function clearError() {
  const box = document.getElementById("errorBox");
  if (box) box.hidden = true;
}
