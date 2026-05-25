const API_BASE = '/api/admin/users';
const DASHBOARD_API = '/api/admin/dashboard';
const PAGE_SIZE = 10;
const CHART_COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

let currentPage = 0;
let currentQuery = '';
let selectedUserId = null;

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
const pageTitle = document.getElementById('pageTitle');
const headerEyebrow = document.getElementById('headerEyebrow');
const userSearchBox = document.getElementById('userSearchBox');
const refreshDashboardBtn = document.getElementById('refreshDashboardBtn');

document.addEventListener('DOMContentLoaded', async () => {
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

    setupEventListeners();
    setupLogout();
    fetchDashboard();
    fetchUsers();
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
    document.querySelectorAll('.nav-item[data-view]').forEach(item => {
        item.addEventListener('click', () => switchView(item.dataset.view));
    });

    refreshDashboardBtn.addEventListener('click', fetchDashboard);

    let timeout;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => {
            currentQuery = e.target.value.trim();
            currentPage = 0;
            fetchUsers();
        }, 500);
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

function switchView(viewId) {
    document.querySelectorAll('.admin-view').forEach(view => view.classList.add('hidden'));
    document.getElementById(viewId).classList.remove('hidden');
    document.querySelectorAll('.nav-item[data-view]').forEach(item => item.classList.toggle('active', item.dataset.view === viewId));

    const isUsers = viewId === 'usersView';
    pageTitle.textContent = isUsers ? 'Gestion de Usuarios' : 'Dashboard Operativo';
    headerEyebrow.textContent = isUsers ? 'Moderacion' : 'Backoffice';
    userSearchBox.classList.toggle('hidden', !isUsers);
    refreshDashboardBtn.classList.toggle('hidden', isUsers);
}

async function fetchDashboard() {
    try {
        refreshDashboardBtn.disabled = true;
        const response = await fetch(DASHBOARD_API, { headers: { 'Accept': 'application/json' } });
        if (!response.ok) throw new Error('Error al cargar dashboard');
        const data = await response.json();
        renderDashboard(data);
    } catch (error) {
        console.error(error);
        showToast('Error al cargar el dashboard', true);
    } finally {
        refreshDashboardBtn.disabled = false;
    }
}

function renderDashboard(data) {
    document.getElementById('dauValue').textContent = formatNumber(data.activeUsers?.dau || 0);
    document.getElementById('mauValue').textContent = `MAU ${formatNumber(data.activeUsers?.mau || 0)}`;
    document.getElementById('latencyValue').textContent = `${Math.round(data.latency?.averageMs || 0)} ms`;
    document.getElementById('latencySamples').textContent = `${formatNumber(data.latency?.samples || 0)} muestras 24h`;
    document.getElementById('errorRateValue').textContent = `${Number(data.errorRate?.percentage || 0).toFixed(2)}%`;
    document.getElementById('errorRateSamples').textContent = `${formatNumber(data.errorRate?.totalRequests || 0)} peticiones 24h`;

    renderPipelineSummary(data.pipelineStatus || {});
    renderPieChart('routePieChart', data.routeTypeDistribution || []);
    renderRouteLegend(data.routeTypeDistribution || []);
    renderZones('topOriginsList', data.topOrigins || []);
    renderZones('topDestinationsList', data.topDestinations || []);
    renderLineChart('activeUsersChart', data.activeUsers?.dailyActiveUsers || [], data.activeUsers?.registrations || []);
    renderIncidentFunnel(data.incidentFunnel || {});
    renderReporters(data.topReporters || []);
    renderPipelineScripts(data.pipelineStatus || {});
    renderLatencyChart(data.latency?.series || []);
}

function renderPipelineSummary(pipeline) {
    const status = pipeline.status || 'UNKNOWN';
    const statusValue = document.getElementById('pipelineStatusValue');
    statusValue.textContent = status;
    statusValue.className = statusClass(status);

    const duration = pipeline.durationMs ? `${Math.round(pipeline.durationMs / 1000)} s` : 'Sin ejecucion';
    document.getElementById('pipelineDuration').textContent = duration;
    document.getElementById('pipelineUpdatedAt').textContent = pipeline.finishedAt || pipeline.startedAt || 'sin datos';
}

function renderPieChart(canvasId, items) {
    const canvas = document.getElementById(canvasId);
    const ctx = canvas.getContext('2d');
    const rect = canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = rect.width * dpr;
    canvas.height = 220 * dpr;
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, rect.width, 220);

    const total = items.reduce((sum, item) => sum + Number(item.total || 0), 0);
    const cx = rect.width / 2;
    const cy = 110;
    const radius = 84;

    if (total === 0) {
        drawEmptyCircle(ctx, cx, cy, radius);
        return;
    }

    let start = -Math.PI / 2;
    items.forEach((item, index) => {
        const value = Number(item.total || 0);
        const slice = (value / total) * Math.PI * 2;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, radius, start, start + slice);
        ctx.closePath();
        ctx.fillStyle = CHART_COLORS[index % CHART_COLORS.length];
        ctx.fill();
        start += slice;
    });

    ctx.beginPath();
    ctx.arc(cx, cy, 44, 0, Math.PI * 2);
    ctx.fillStyle = '#1e293b';
    ctx.fill();
    ctx.fillStyle = '#f8fafc';
    ctx.font = '700 22px Inter';
    ctx.textAlign = 'center';
    ctx.fillText(formatNumber(total), cx, cy + 6);
}

function drawEmptyCircle(ctx, cx, cy, radius) {
    ctx.beginPath();
    ctx.arc(cx, cy, radius, 0, Math.PI * 2);
    ctx.strokeStyle = 'rgba(255,255,255,0.16)';
    ctx.lineWidth = 18;
    ctx.stroke();
    ctx.fillStyle = '#94a3b8';
    ctx.font = '600 13px Inter';
    ctx.textAlign = 'center';
    ctx.fillText('Sin datos', cx, cy + 4);
}

function renderRouteLegend(items) {
    const total = items.reduce((sum, item) => sum + Number(item.total || 0), 0);
    const legend = document.getElementById('routeLegend');
    legend.innerHTML = items.map((item, index) => {
        const pct = total ? Math.round((Number(item.total || 0) / total) * 100) : 0;
        return `
            <div class="legend-item">
                <div class="legend-left">
                    <span class="item-title"><span class="legend-dot" style="background:${CHART_COLORS[index % CHART_COLORS.length]}"></span>${escapeHtml(item.label || item.routeType)}</span>
                    <span class="item-subtitle">${pct}% del total</span>
                </div>
                <span class="item-value">${formatNumber(item.total || 0)}</span>
            </div>
        `;
    }).join('');
}

function renderZones(containerId, zones) {
    const container = document.getElementById(containerId);
    const max = Math.max(...zones.map(z => Number(z.total || 0)), 1);
    if (zones.length === 0) {
        container.innerHTML = `<div class="zone-item"><span class="item-subtitle">Sin datos</span></div>`;
        return;
    }

    container.innerHTML = zones.map(zone => {
        const width = Math.max(8, Math.round((Number(zone.total || 0) / max) * 100));
        return `
            <div class="zone-item">
                <div class="zone-left">
                    <span class="item-title">${escapeHtml(zone.zone || 'Sin zona')}</span>
                    <span class="item-subtitle">${formatCoord(zone.lat)}, ${formatCoord(zone.lon)}</span>
                    <div class="zone-bar"><span style="width:${width}%"></span></div>
                </div>
                <span class="item-value">${formatNumber(zone.total || 0)}</span>
            </div>
        `;
    }).join('');
}

function renderLineChart(canvasId, activeSeries, registrationSeries) {
    const canvas = document.getElementById(canvasId);
    const ctx = prepareCanvas(canvas, 220);
    const active = normalizeSeries(activeSeries);
    const registrations = normalizeSeries(registrationSeries);
    drawLines(ctx, canvas, [
        { label: 'DAU', data: active, color: '#3b82f6' },
        { label: 'Registros', data: registrations, color: '#10b981' }
    ], 220);
}

function renderLatencyChart(series) {
    const canvas = document.getElementById('latencyChart');
    const ctx = prepareCanvas(canvas, 180);
    drawLines(ctx, canvas, [{ label: 'ms', data: normalizeSeries(series), color: '#f59e0b' }], 180);
}

function prepareCanvas(canvas, height) {
    const rect = canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = rect.width * dpr;
    canvas.height = height * dpr;
    const ctx = canvas.getContext('2d');
    ctx.scale(dpr, dpr);
    ctx.clearRect(0, 0, rect.width, height);
    return ctx;
}

function drawLines(ctx, canvas, lines, height) {
    const width = canvas.getBoundingClientRect().width;
    // Aumentamos el padding izquierdo (padLeft) a 45 para dar espacio a los números
    const padLeft = 45;
    const padRight = 28;
    const padTopBottom = 28;
    const allValues = lines.flatMap(line => line.data.map(point => point.value));
    const maxValue = Math.max(...allValues, 1);
    const pointCount = Math.max(...lines.map(line => line.data.length), 2);

    ctx.strokeStyle = 'rgba(255,255,255,0.08)';
    ctx.lineWidth = 1;
    ctx.fillStyle = '#ffffff';
    ctx.font = '500 11px Inter';
    ctx.textAlign = 'right';
    ctx.textBaseline = 'middle';

    // Dibujar las 4 líneas guía horizontales y sus valores en el eje Y
    for (let i = 0; i < 4; i++) {
        const y = padTopBottom + ((height - padTopBottom * 2) / 3) * i;

        // Dibujar la línea guía
        ctx.beginPath();
        ctx.moveTo(padLeft, y);
        ctx.lineTo(width - padRight, y);
        ctx.stroke();

        // Calcular y dibujar el valor del eje Y
        const valueAtY = maxValue - (maxValue / 3) * i;
        // Redondear el número y añadir sufijo 'k' si es mayor a 1000 para que quepa bien
        let formattedValue = valueAtY >= 1000 ? (valueAtY / 1000).toFixed(1) + 'k' : Math.round(valueAtY);
        ctx.fillText(formattedValue, padLeft - 8, y);
    }

    lines.forEach(line => {
        if (line.data.length === 0) return;
        ctx.beginPath();
        line.data.forEach((point, index) => {
            const x = padLeft + ((width - padLeft - padRight) / (pointCount - 1)) * index;
            const y = height - padTopBottom - ((height - padTopBottom * 2) * (point.value / maxValue));
            if (index === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        });
        ctx.strokeStyle = line.color;
        ctx.lineWidth = 2;
        ctx.stroke();
    });

    if (allValues.length === 0) {
        ctx.fillStyle = '#94a3b8';
        ctx.font = '600 13px Inter';
        ctx.textAlign = 'center';
        ctx.fillText('Sin datos', width / 2, height / 2);
    }
}

function normalizeSeries(series) {
    return (series || []).map(point => ({ label: point.label, value: Number(point.value || 0) }));
}

function renderIncidentFunnel(funnel) {
    const total = Number(funnel.total || 0);
    const steps = [
        ['Total creadas', total, '#3b82f6'],
        ['Validadas', Number(funnel.accepted || 0), '#10b981'],
        ['Rechazadas', Number(funnel.rejected || 0), '#ef4444'],
        ['Pendientes', Number(funnel.pending || 0), '#f59e0b']
    ];

    document.getElementById('incidentFunnel').innerHTML = steps.map(([label, value, color]) => {
        const width = total ? Math.max(4, Math.round((value / total) * 100)) : 0;
        return `
            <div class="funnel-step">
                <div class="funnel-top"><span>${label}</span><strong>${formatNumber(value)}</strong></div>
                <div class="funnel-bar"><span style="width:${width}%; background:${color}"></span></div>
            </div>
        `;
    }).join('');
}

function renderReporters(reporters) {
    const container = document.getElementById('reportersList');
    if (reporters.length === 0) {
        container.innerHTML = `<div class="ranking-item"><span class="item-subtitle">Sin datos</span></div>`;
        return;
    }

    container.innerHTML = reporters.map((reporter, index) => `
        <div class="ranking-item">
            <div class="ranking-left">
                <span class="item-title">#${index + 1} ${escapeHtml(reporter.username || reporter.googleId)}</span>
                <span class="item-subtitle">Nivel ${reporter.level || 0} · ${formatNumber(reporter.points || 0)} pts</span>
            </div>
            <span class="item-value">${formatNumber(reporter.incidents || 0)}</span>
        </div>
    `).join('');
}

function renderPipelineScripts(pipeline) {
    const container = document.getElementById('pipelineScripts');
    const scripts = pipeline.scripts || [];
    if (scripts.length === 0) {
        container.innerHTML = `<div class="script-item"><span class="item-subtitle">${pipeline.errorMessage || 'Sin datos de scripts'}</span></div>`;
        return;
    }

    container.innerHTML = scripts.map(script => `
        <div class="script-item">
            <div class="script-left">
                <span class="item-title">${escapeHtml(script.scriptName)}</span>
                <span class="item-subtitle">${Math.round((script.durationMs || 0) / 1000)} s · exit ${script.exitCode ?? '-'}</span>
            </div>
            <span class="status-pill ${statusClass(script.status)}">${script.status}</span>
        </div>
    `).join('');
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
        tableBody.innerHTML = `<tr><td colspan="6" class="loading-cell" style="color: var(--danger)">Error de conexion.</td></tr>`;
        showToast('Error al cargar la lista de usuarios', true);
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
        let badgeClass = 'badge-active';
        if (user.status === 'SUSPENDED') badgeClass = 'badge-suspended';
        if (user.status === 'BANNED') badgeClass = 'badge-banned';

        const initial = user.username ? user.username.charAt(0).toUpperCase() : '?';
        tr.innerHTML = `
            <td>#${user.id}</td>
            <td>
                <div class="user-info">
                    <div class="user-avatar">${initial}</div>
                    <span>${escapeHtml(user.username)}</span>
                </div>
            </td>
            <td>${escapeHtml(user.email)}</td>
            <td>Nvl ${user.level} <span style="color:var(--text-secondary);font-size:0.8rem">(${user.points} pts)</span></td>
            <td><span class="badge ${badgeClass}">${user.status}</span></td>
            <td><button class="btn btn-secondary btn-sm" onclick="openModal(${user.id})">Gestionar</button></td>
        `;
        tableBody.appendChild(tr);
    });

    updatePagination(pageData);
}

function updatePagination(pageData) {
    btnPrev.disabled = pageData.first;
    btnNext.disabled = pageData.last;
    const totalPages = pageData.totalPages === 0 ? 1 : pageData.totalPages;
    pageInfo.textContent = `Pagina ${pageData.number + 1} de ${totalPages}`;
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
        modalBody.innerHTML = `<p style="text-align:center; color: var(--danger)">No se pudo cargar la informacion.</p>`;
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
                <h4 style="font-size:1.2rem; margin-bottom:4px;">${escapeHtml(user.username)}</h4>
                <p style="color:var(--text-secondary); font-size:0.9rem;">${escapeHtml(user.email)}</p>
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
                <label>Indice de Confianza</label>
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
                const date = new Date(inc.createdAt).toLocaleDateString('es-ES', {
                    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
                });

                return `
                    <div class="incident-card">
                        <div class="incident-header">
                            <span class="incident-type">${escapeHtml(inc.type)}</span>
                            <span class="incident-date">${date}</span>
                        </div>
                        <p class="incident-desc">${escapeHtml(inc.description || 'Sin descripcion')}</p>
                        <div class="incident-footer">
                            <span class="incident-votes positive">+ ${inc.positiveVotes}</span>
                            <span class="incident-votes negative">- ${inc.negativeVotes}</span>
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

function statusClass(status) {
    const normalized = String(status || '').toUpperCase();
    if (normalized === 'SUCCESS') return 'status-success';
    if (normalized === 'FAILED') return 'status-failed';
    if (normalized === 'RUNNING') return 'status-running';
    return 'status-unknown';
}

function formatNumber(value) {
    return Number(value || 0).toLocaleString('es-ES');
}

function formatCoord(value) {
    return Number.isFinite(Number(value)) ? Number(value).toFixed(4) : '-';
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function showToast(message, isError = false) {
    toastEl.textContent = message;
    toastEl.style.borderLeftColor = isError ? 'var(--danger)' : 'var(--success)';
    toastEl.classList.remove('hidden');

    setTimeout(() => {
        toastEl.classList.add('hidden');
    }, 3000);
}
