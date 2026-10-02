const form = document.querySelector('#transaction-form');
const resultContent = document.querySelector('#result-content');
const historyBody = document.querySelector('#history-body');
const historyEmpty = document.querySelector('#history-empty');
const clearHistoryButton = document.querySelector('#clear-history');
const submitButton = document.querySelector('#submit-button');
const submitLabel = document.querySelector('#submit-label');
const formError = document.querySelector('#form-error');
const toast = document.querySelector('#toast');
const apiKeyInput = document.querySelector('#api-key');
let toastTimeout;

function localDateTimeValue(date = new Date()) {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function newTransactionId() {
    const stamp = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(2, 12);
    return `TX-${stamp}-${Math.floor(Math.random() * 90 + 10)}`;
}

try {
    localStorage.removeItem('fraud-detection-history-v1');
} catch {
    // Storage may be disabled by the browser.
}

let history = [];

function safeText(value) {
    return String(value ?? '').replace(/[&<>"']/g, (character) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[character]);
}

function formatAmount(amount, currency) {
    return new Intl.NumberFormat('it-IT', { style: 'currency', currency }).format(amount);
}

function formatTime(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Data non valida';
    return new Intl.DateTimeFormat('it-IT', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date);
}

function updateMetrics() {
    const total = history.length;
    const blocked = history.filter((item) => item.decision?.status === 'BLOCKED').length;
    const approved = total - blocked;
    document.querySelector('#metric-total').textContent = total;
    document.querySelector('#metric-blocked').textContent = blocked;
    document.querySelector('#metric-approved').textContent = total ? `${Math.round((approved / total) * 100)}%` : '0%';
}

function renderHistory() {
    historyBody.innerHTML = history.slice(0, 8).map((item) => {
        const blocked = item.decision?.status === 'BLOCKED';
        const status = blocked ? 'Bloccata' : 'Approvata';
        return `<tr>
      <td class="cell-id">${safeText(item.transaction.transactionId)}</td>
      <td>${safeText(item.transaction.userId)}</td>
      <td>${safeText(formatAmount(item.transaction.amount, item.transaction.currency))}</td>
      <td><span class="decision-pill${blocked ? ' blocked' : ''}"><span class="pill-dot"></span>${status}</span></td>
      <td class="risk-cell">${safeText(item.decision?.riskScore ?? 0)} / 100</td>
      <td>${safeText(formatTime(item.savedAt))}</td>
    </tr>`;
    }).join('');
    const hasHistory = history.length > 0;
    historyEmpty.hidden = hasHistory;
    clearHistoryButton.disabled = !hasHistory;
    updateMetrics();
}

function renderDecision(decision) {
    const blocked = decision.status === 'BLOCKED';
    const score = Math.max(0, Number(decision.riskScore) || 0);
    const meter = Math.max(0, Math.min(score, 100));
    const scoreClass = score >= 70 ? 'high' : score >= 35 ? 'elevated' : '';
    const alert = decision.alert;
    const details = alert?.ruleTriggeredDescription
        ? `<div class="alert-detail"><h3>REGOLA ATTIVATA</h3><p>${safeText(alert.ruleTriggeredDescription)}</p></div>`
        : '<div class="alert-empty">Nessuna regola di rischio attivata. Transazione approvata.</div>';
    resultContent.innerHTML = `<div class="result-summary">
    <div class="decision-banner${blocked ? ' blocked' : ''}">
      <div><div class="decision-label">ESITO ANALISI</div><div class="decision-value">${blocked ? 'Transazione bloccata' : 'Transazione approvata'}</div></div>
      <div class="decision-mark" aria-hidden="true">${blocked ? '!' : '✓'}</div>
    </div>
    <div class="score-block">
      <div class="score-header"><span class="score-caption">PUNTEGGIO DI RISCHIO</span><span class="score-number">${safeText(score)}<small> / 100</small></span></div>
    <progress class="score-track ${scoreClass}" value="${meter}" max="100" aria-label="Punteggio di rischio ${safeText(score)} su 100"></progress>
      <div class="score-scale"><span>BASSO</span><span>MEDIO</span><span>ALTO</span></div>
    </div>
    ${details}
    ${alert?.alertId ? `<div class="alert-meta">ALERT ${safeText(alert.alertId)} · ${safeText(formatTime(alert.timestamp))}</div>` : ''}
  </div>`;
}

function showToast(message, isError = false) {
    clearTimeout(toastTimeout);
    toast.textContent = message;
    toast.classList.toggle('error', isError);
    toast.classList.add('show');
    toastTimeout = setTimeout(() => toast.classList.remove('show'), 3200);
}

function showFormError(message) {
    formError.textContent = message;
    formError.hidden = !message;
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    showFormError('');
    if (!form.reportValidity()) return;

    const formData = new FormData(form);
    const latitude = formData.get('latitude');
    const longitude = formData.get('longitude');
    if ((latitude && !longitude) || (!latitude && longitude)) {
        showFormError('Inserisci entrambe le coordinate oppure lascia vuoti latitudine e longitudine.');
        return;
    }

    const transaction = {
        transactionId: formData.get('transactionId').trim(),
        userId: formData.get('userId').trim(),
        amount: Number(formData.get('amount')),
        currency: formData.get('currency'),
        timestamp: new Date(formData.get('timestamp')).toISOString(),
        merchantCategory: formData.get('merchantCategory'),
        location: {
            city: formData.get('city').trim(),
            ...(latitude && longitude ? { latitude: Number(latitude), longitude: Number(longitude) } : {})
        }
    };

    submitButton.disabled = true;
    submitLabel.textContent = 'Analisi in corso...';
    try {
        const headers = { 'Content-Type': 'application/json' };
        if (apiKeyInput.value) headers.Authorization = `Bearer ${apiKeyInput.value}`;
        const response = await fetch('/api/v1/transactions', {
            method: 'POST',
            headers,
            body: JSON.stringify(transaction)
        });
        const responseBody = await response.json().catch(() => ({}));
        if (!response.ok) {
            const details = Array.isArray(responseBody.details) ? responseBody.details.join(' · ') : '';
            throw new Error(details || responseBody.message || `Errore del server (HTTP ${response.status}).`);
        }

        renderDecision(responseBody);
        history.unshift({ transaction, decision: responseBody, savedAt: new Date().toISOString() });
        history = history.slice(0, 30);
        renderHistory();
        document.querySelector('#transactionId').value = newTransactionId();
        showToast(responseBody.status === 'BLOCKED' ? 'Rilevata una transazione ad alto rischio.' : 'Transazione analizzata correttamente.');
    } catch (error) {
        const message = error instanceof TypeError
            ? 'Server non raggiungibile. Avvia l’app con Maven e riprova.'
            : error.message;
        showFormError(message);
        showToast('Analisi non riuscita.', true);
    } finally {
        submitButton.disabled = false;
        submitLabel.textContent = 'Analizza transazione';
    }
});

document.querySelector('#reset-form').addEventListener('click', () => {
    form.reset();
    document.querySelector('#transactionId').value = newTransactionId();
    document.querySelector('#timestamp').value = localDateTimeValue();
    document.querySelector('#city').value = 'Milano';
    document.querySelector('#amount').value = '6200.00';
    showFormError('');
});

clearHistoryButton.addEventListener('click', () => {
    history = [];
    renderHistory();
    showToast('Registro locale cancellato.');
});

document.querySelector('#timestamp').value = localDateTimeValue();
document.querySelector('#transactionId').value = newTransactionId();
renderHistory();
