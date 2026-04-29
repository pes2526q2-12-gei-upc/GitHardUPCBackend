const API_BASE = '/api/admin/users';

// State
let currentPage = 0;
let currentQuery = '';
let selectedUserId = null;
const PAGE_SIZE = 10;

// DOM Elements
const searchInput = document.getElementById('searchInput');
const tableBody = document.getElementById('userTableBody');
const btnPrev = document.getElementById('btnPrev');
const btnNext = document.getElementById('btnNext');
const pageInfo = document.getElementById('pageInfo');

const userModal = document.getElementById('userModal');
const closeModalBtn = document.getElementById('closeModal');
const modalBody = document.getElementById('modalBody');
const toastEl = document.getElementById('toast');

const btnActive = document.getElementById('btnActive');
const btnSuspend = document.getElementById('btnSuspend');
const btnBan = document.getElementById('btnBan');

// Initialize
document.addEventListener('DOMContentLoaded', async () => {
    // Comprovem si hi ha sessio activa. Si no, redirigim al login.
    try {
        const authRes = await fetch('/admin/check-auth', { headers: { 'Accept': 'application/json' } });
        if (!authRes.ok) {
            window.location.href = '/admin/login.html';
            return;
        }
    } catch (err) {
        window.location.href = '/admin/login.html';
        return;
    }

    fetchUsers();
    setupEventListeners();
    setupLogout();
});

function setupLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (!logoutBtn) return;
    logoutBtn.addEventListener('click', async () => {
        try {
            await fetch('/admin/logout', { method: 'POST' });
        } finally {
            window.location.href = '/admin/login.html';
        }
    });
}

function setupEventListeners() {
    let timeout;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => {
            currentQuery = e.target.value.trim();
            currentPage = 0;
            fetchUsers();
        }, 500); // Debounce
    });

    btnPrev.addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            fetchUsers();
        }
    });

    btnNext.addEventListener('click', () => {
        currentPage++;
        fetchUsers();
    });

    closeModalBtn.addEventListener('click', closeModal);
    userModal.addEventListener('click', (e) => {
        if (e.target === userModal) closeModal();
    });

    btnActive.addEventListener('click', () => updateStatus('ACTIVE'));
    btnSuspend.addEventListener('click', () => updateStatus('SUSPENDED'));
    btnBan.addEventListener('click', () => updateStatus('BANNED'));
}

async function fetchUsers() {
    try {
        tableBody.innerHTML = `<tr><td colspan="6" class="loading-cell">Cargando usuarios...</td></tr>`;
        
        const url = `${API_BASE}?page=${currentPage}&size=${PAGE_SIZE}${currentQuery ? '&query=' + encodeURIComponent(currentQuery) : ''}`;
        const response = await fetch(url);
        
        if (!response.ok) throw new Error('Error al cargar usuarios');
        
        const data = await response.json();
        renderTable(data);
    } catch (error) {
        console.error(error);
        tableBody.innerHTML = `<tr><td colspan="6" class="loading-cell" style="color: var(--danger)">Error de conexión. Asegúrate de que el backend está corriendo.</td></tr>`;
        showToast('Error al cargar la lista de usuarios');
    }
}

function renderTable(pageData) {
    tableBody.innerHTML = '';
    
    if (pageData.content.length === 0) {
        tableBody.innerHTML = `<tr><td colspan="6" class="loading-cell">No se encontraron usuarios.</td></tr>`;
        updatePagination(pageData);
        return;
    }

    pageData.content.forEach(user => {
        const tr = document.createElement('tr');
        
        // Badge color based on status
        let badgeClass = 'badge-active';
        if (user.status === 'SUSPENDED') badgeClass = 'badge-suspended';
        if (user.status === 'BANNED') badgeClass = 'badge-banned';

        const initial = user.username ? user.username.charAt(0).toUpperCase() : '?';

        tr.innerHTML = `
            <td>#${user.id}</td>
            <td>
                <div class="user-info">
                    <div class="user-avatar">${initial}</div>
                    <span>${user.username}</span>
                </div>
            </td>
            <td>${user.email}</td>
            <td>Nvl ${user.level} <span style="color:var(--text-secondary);font-size:0.8rem">(${user.points} pts)</span></td>
            <td><span class="badge ${badgeClass}">${user.status}</span></td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="openModal(${user.id})">Gestionar</button>
            </td>
        `;
        tableBody.appendChild(tr);
    });

    updatePagination(pageData);
}

function updatePagination(pageData) {
    btnPrev.disabled = pageData.first;
    btnNext.disabled = pageData.last;
    
    const totalPages = pageData.totalPages === 0 ? 1 : pageData.totalPages;
    pageInfo.textContent = `Página ${pageData.number + 1} de ${totalPages}`;
}

async function openModal(id) {
    selectedUserId = id;
    modalBody.innerHTML = `<p style="text-align:center; color: var(--text-secondary)">Cargando perfil...</p>`;
    document.getElementById('incidentsContainer').classList.add('hidden');
    userModal.classList.remove('hidden');

    try {
        const response = await fetch(`${API_BASE}/${id}`);
        if (!response.ok) throw new Error('Error al cargar perfil');
        
        const user = await response.json();
        renderModalContent(user);
    } catch (error) {
        modalBody.innerHTML = `<p style="text-align:center; color: var(--danger)">No se pudo cargar la información.</p>`;
    }
}

function renderModalContent(user) {
    let badgeClass = 'badge-active';
    if (user.status === 'SUSPENDED') badgeClass = 'badge-suspended';
    if (user.status === 'BANNED') badgeClass = 'badge-banned';

    modalBody.innerHTML = `
        <div style="display:flex; align-items:center; gap: 1rem; margin-bottom: 2rem;">
            <div class="user-avatar" style="width: 64px; height: 64px; font-size: 1.5rem;">
                ${user.username ? user.username.charAt(0).toUpperCase() : '?'}
            </div>
            <div>
                <h4 style="font-size:1.2rem; margin-bottom:4px;">${user.username}</h4>
                <p style="color:var(--text-secondary); font-size:0.9rem;">${user.email}</p>
                <div style="margin-top: 8px;"><span class="badge ${badgeClass}">${user.status}</span></div>
            </div>
        </div>

        <div class="user-profile-grid">
            <div class="profile-stat">
                <label>Nivel de Usuario</label>
                <div class="value">${user.level}</div>
            </div>
            <div class="profile-stat">
                <label>Puntos Actuales</label>
                <div class="value">${user.points}</div>
            </div>
            <div class="profile-stat">
                <label>Índice de Confianza (Reputación)</label>
                <div class="value" style="color: var(--accent-color)">${user.reputacio}</div>
            </div>
            <div class="profile-stat">
                <label>Historial Incidencias</label>
                <div class="value" style="font-size: 0.9rem;">
                    <a href="#" onclick="viewIncidents(${user.id})" style="color: var(--accent-color); text-decoration: none;">Ver Registro</a>
                </div>
            </div>
        </div>
    `;

    // Reset button visibility based on current status
    btnActive.style.display = user.status !== 'ACTIVE' ? 'block' : 'none';
    btnSuspend.style.display = user.status !== 'SUSPENDED' ? 'block' : 'none';
    btnBan.style.display = user.status !== 'BANNED' ? 'block' : 'none';
}

function closeModal() {
    userModal.classList.add('hidden');
    document.getElementById('incidentsContainer').classList.add('hidden');
    selectedUserId = null;
}

async function updateStatus(newStatus) {
    if (!selectedUserId) return;

    // Optimistic UI could be implemented here, but we wait for response for safety
    const btnMap = { 'ACTIVE': btnActive, 'SUSPENDED': btnSuspend, 'BANNED': btnBan };
    const originalText = btnMap[newStatus].textContent;
    btnMap[newStatus].textContent = 'Procesando...';
    btnMap[newStatus].disabled = true;

    try {
        const response = await fetch(`${API_BASE}/${selectedUserId}/status`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });

        if (!response.ok) throw new Error('Error al actualizar estado');
        
        const updatedUser = await response.json();
        showToast(`Usuario ${updatedUser.username} cambiado a ${newStatus}`);
        
        // Refresh
        fetchUsers();
        renderModalContent(updatedUser);
    } catch (error) {
        showToast('Hubo un error al actualizar el estado', true);
    } finally {
        btnMap[newStatus].textContent = originalText;
        btnMap[newStatus].disabled = false;
    }
}

async function viewIncidents(userId) {
    const incidentsContainer = document.getElementById('incidentsContainer');
    const incidentsList = document.getElementById('incidentsList');
    
    // Toggle visibility if already open
    if (!incidentsContainer.classList.contains('hidden')) {
        incidentsContainer.classList.add('hidden');
        return;
    }

    try {
        incidentsList.innerHTML = `<p style="text-align:center; color: var(--text-secondary)">Cargando incidencias...</p>`;
        incidentsContainer.classList.remove('hidden');

        const response = await fetch(`${API_BASE}/${userId}/incidents`);
        if (!response.ok) throw new Error('Error de red');
        const incidents = await response.json();
        
        if (incidents.length === 0) {
            incidentsList.innerHTML = `<p style="text-align:center; color: var(--text-secondary); padding: 1rem;">Este usuario no tiene incidencias registradas.</p>`;
        } else {
            incidentsList.innerHTML = incidents.map(inc => {
                const date = new Date(inc.created).toLocaleDateString('es-ES', { 
                    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' 
                });
                
                return `
                    <div class="incident-card">
                        <div class="incident-header">
                            <span class="incident-type">${inc.type}</span>
                            <span class="incident-date">${date}</span>
                        </div>
                        <p class="incident-desc">${inc.description || 'Sin descripción'}</p>
                        <div class="incident-footer">
                            <span class="incident-votes positive">👍 ${inc.positiveVotes}</span>
                            <span class="incident-votes negative">👎 ${inc.negativeVotes}</span>
                            <span style="margin-left: auto;">Fiabilidad: ${inc.reliabilityIndex}</span>
                        </div>
                    </div>
                `;
            }).join('');
        }
    } catch(err) {
        incidentsList.innerHTML = `<p style="text-align:center; color: var(--danger)">Error al cargar las incidencias.</p>`;
    }
}

function showToast(message, isError = false) {
    toastEl.textContent = message;
    toastEl.style.borderLeftColor = isError ? 'var(--danger)' : 'var(--success)';
    toastEl.classList.remove('hidden');
    
    setTimeout(() => {
        toastEl.classList.add('hidden');
    }, 3000);
}
