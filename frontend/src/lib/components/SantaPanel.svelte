<!-- frontend/src/lib/components/SantaPanel.svelte -->
<script>
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  // Admin panel state
  let showAdminPanel = false;
  let adminGameCode = '';
  let adminLoading = false;
  let adminError = null;
  let adminSuccess = false;
  let participants = [];
  let loadingParticipants = false;

  // Fetch participants for a game
  async function fetchParticipants(gameCode) {
    if (!gameCode.trim()) return;

    loadingParticipants = true;

    try {
      const response = await fetch(`http://localhost:8080/api/games/${gameCode.toUpperCase()}/participants`);

      if (!response.ok) {
        participants = [];
        return;
      }

      const data = await response.json();
      participants = data.participants || data || [];
    } catch (error) {
      console.error('Error fetching participants:', error);
      participants = [];
    } finally {
      loadingParticipants = false;
    }
  }

  // Watch for game code changes
  $: if (adminGameCode && adminGameCode.length >= 3) {
    fetchParticipants(adminGameCode);
  }

  // Format email for display (partially masked)
  function formatEmail(email) {
    if (!email) return '';

    const [username, domain] = email.split('@');
    if (!username || !domain) return email;

    // Keep first 3 characters, mask the rest before @
    const maskedUsername = username.length > 3
      ? username.substring(0, 3) + '*'.repeat(Math.min(username.length - 3, 4))
      : username;

    return `${maskedUsername}@${domain}`;
  }

  // Assign secret santas
  async function assignSecretSantas() {
    if (!adminGameCode.trim()) {
      adminError = 'Ingresa el código del juego';
      return;
    }

    adminLoading = true;
    adminError = null;

    try {
      const response = await fetch(`http://localhost:8080/api/games/${adminGameCode.toUpperCase()}/assign`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Error (${response.status}): ${errText}`);
      }

      adminSuccess = true;
      setTimeout(() => {
        showAdminPanel = false;
        adminSuccess = false;
        adminGameCode = '';
        participants = [];
        dispatch('assigned', { gameCode: adminGameCode.toUpperCase() });
      }, 10000);

    } catch (err) {
      adminError = err.message.includes('404')
        ? 'Juego no encontrado'
        : err.message.includes('menos')
        ? 'Se necesitan al menos 3 participantes'
        : 'Error al asignar amigos secretos';
    } finally {
      adminLoading = false;
    }
  }

  // Close panel
  function closePanel() {
    showAdminPanel = false;
    adminError = null;
    adminSuccess = false;
    participants = [];
    adminGameCode = '';
  }
</script>

<!-- Admin button -->
<button
  class="admin-button"
  on:click={() => showAdminPanel = true}
  title="Panel de Santa - Asignar amigos secretos"
>
  🎅 Panel Santa
</button>

<!-- Admin panel modal -->
{#if showAdminPanel}
  <div class="admin-modal-overlay" on:click|self={closePanel}>
    <div class="admin-modal" on:click|stopPropagation>
      <button class="close-button" on:click={closePanel}>×</button>

      <h2>🎅 Panel de Santa</h2>
      <p class="modal-subtitle">Asigna los amigos secretos para un juego</p>

      {#if adminSuccess}
        <div class="success-message">
          <div class="success-icon">✅</div>
          <h3>¡Asignación Exitosa!</h3>
          <p>Los amigos secretos han sido asignados y los emails están siendo enviados.</p>
          <p class="note">El panel se cerrará automáticamente...</p>
        </div>
      {:else}
        <div class="admin-form">
          {#if adminError}
            <div class="error-message">
              ⚠️ {adminError}
            </div>
          {/if}

          <div class="form-group">
            <label for="adminGameCode">
              <span class="icon">🔑</span> Código del Juego
            </label>
            <input
              id="adminGameCode"
              type="text"
              bind:value={adminGameCode}
              placeholder="Ej: X8B3K9"
              required
              maxlength="10"
              disabled={adminLoading}
            />
            <div class="hint">Ingresa el código del juego que quieres asignar</div>
          </div>

          <!-- Participants list -->
          {#if adminGameCode && adminGameCode.length >= 3}
            <div class="participants-section">
              <h3>
                <span class="section-icon">👥</span>
                Participantes del Juego
                {#if loadingParticipants}
                  <span class="loading-spinner-small"></span>
                {:else if participants.length > 0}
                  <span class="participant-count">({participants.length})</span>
                {/if}
              </h3>

              {#if loadingParticipants}
                <div class="loading-state">
                  <div class="loading-spinner"></div>
                  <p>Cargando participantes...</p>
                </div>
              {:else if participants.length === 0}
                <div class="empty-state">
                  <span class="empty-icon">👻</span>
                  <p>No hay participantes en este juego</p>
                  <p class="note">Verifica que el código sea correcto</p>
                </div>
              {:else}
                <div class="participants-list">
                  {#each participants as participant, index}
                    <div class="participant-item">
                      <div class="participant-number">#{index + 1}</div>
                      <div class="participant-info">
                        <div class="participant-name">{participant.name || 'Sin nombre'}</div>
                        <div class="participant-email">{formatEmail(participant.email)}</div>
                      </div>
                      <div class="participant-status">
                        {#if participant.assigned}
                          <span class="assigned-badge">🎁 Asignado</span>
                        {:else}
                          <span class="pending-badge">⏳ Pendiente</span>
                        {/if}
                      </div>
                    </div>
                  {/each}
                </div>

                <div class="participants-summary">
                  <div class="summary-item">
                    <span class="summary-label">Total:</span>
                    <span class="summary-value">{participants.length} participantes</span>
                  </div>
                  <div class="summary-item">
                    <span class="summary-label">Asignados:</span>
                    <span class="summary-value">
                      {participants.filter(p => p.assigned).length}
                    </span>
                  </div>
                  <div class="summary-item">
                    <span class="summary-label">Pendientes:</span>
                    <span class="summary-value">
                      {participants.filter(p => !p.assigned).length}
                    </span>
                  </div>
                </div>
              {/if}
            </div>
          {/if}

          <div class="requirements">
            <h4>📋 Requisitos para asignar:</h4>
            <ul>
              <li>✅ Al menos 3 participantes únicos</li>
              <li>✅ Juego no asignado previamente</li>
              <li>✅ Emails válidos de todos los participantes</li>
            </ul>
            <p class="note">Cada participante recibirá un email con su amigo secreto asignado.</p>
          </div>

          <div class="action-buttons">
            <button
              on:click={assignSecretSantas}
              disabled={adminLoading || !adminGameCode.trim() || participants.length < 3}
              class="assign-button"
            >
              {#if adminLoading}
                <span class="spinner"></span> Asignando...
              {:else}
                🎲 Asignar Amigos Secretos
              {/if}
            </button>

            <button
              on:click={closePanel}
              class="cancel-button"
              disabled={adminLoading}
            >
              Cancelar
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  /* Admin button */
  .admin-button {
    position: absolute;
    top: 1rem;
    right: 1rem;
    background: linear-gradient(135deg, #c53030, #e53e3e);
    color: white;
    border: none;
    border-radius: 20px;
    padding: 0.6rem 1.2rem;
    font-size: 0.9rem;
    font-weight: 600;
    cursor: pointer;
    z-index: 100;
    box-shadow:
      0 3px 10px rgba(0, 0, 0, 0.3),
      0 0 10px rgba(197, 48, 48, 0.3);
    transition: all 0.3s ease;
    border: 2px solid rgba(255, 215, 0, 0.3);
  }

  .admin-button:hover {
    background: linear-gradient(135deg, #9b2c2c, #c53030);
    transform: translateY(-2px);
    box-shadow:
      0 5px 15px rgba(0, 0, 0, 0.4),
      0 0 15px rgba(197, 48, 48, 0.4);
  }

  /* Admin modal */
  .admin-modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.8);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    backdrop-filter: blur(5px);
  }

  .admin-modal {
    background: white;
    border-radius: 16px;
    padding: 2rem;
    width: 90%;
    max-width: 600px;
    color: #2d3748;
    position: relative;
    box-shadow:
      0 10px 30px rgba(0, 0, 0, 0.4),
      0 0 20px rgba(255, 215, 0, 0.2);
    border: 3px solid #c53030;
    max-height: 90vh;
    overflow-y: auto;
  }

  .close-button {
    position: absolute;
    top: 1rem;
    right: 1rem;
    background: none;
    border: none;
    font-size: 1.5rem;
    color: #718096;
    cursor: pointer;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
  }

  .close-button:hover {
    background: #f7fafc;
    color: #4a5568;
  }

  .admin-modal h2 {
    color: #c53030;
    text-align: center;
    margin-bottom: 0.5rem;
  }

  .modal-subtitle {
    text-align: center;
    color: #718096;
    margin-bottom: 1.5rem;
    font-size: 0.95rem;
  }

  /* Admin form styles */
  .admin-form .form-group {
    margin-bottom: 1.5rem;
  }

  .admin-form label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #2d3748;
  }

  .admin-form input {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e0;
    border-radius: 8px;
    font-size: 1rem;
    box-sizing: border-box;
    transition: all 0.2s ease;
  }

  .admin-form input:focus {
    outline: none;
    border-color: #c53030;
    box-shadow: 0 0 0 3px rgba(197, 48, 48, 0.1);
  }

  .hint {
    font-size: 0.85rem;
    color: #718096;
    margin-top: 0.3rem;
    font-style: italic;
  }

  /* Participants section */
  .participants-section {
    margin: 1.5rem 0;
    background: #f8fafc;
    border-radius: 12px;
    padding: 1.5rem;
    border: 1px solid #e2e8f0;
  }

  .participants-section h3 {
    color: #2d3748;
    margin-top: 0;
    margin-bottom: 1rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 1.1rem;
  }

  .section-icon {
    font-size: 1.2rem;
  }

  .participant-count {
    background: #c53030;
    color: white;
    font-size: 0.8rem;
    padding: 0.2rem 0.6rem;
    border-radius: 12px;
    margin-left: auto;
  }

  /* Loading state */
  .loading-state {
    text-align: center;
    padding: 2rem;
    color: #718096;
  }

  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid #e2e8f0;
    border-top: 3px solid #c53030;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 1rem;
  }

  .loading-spinner-small {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid #e2e8f0;
    border-top: 2px solid #c53030;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-left: 0.5rem;
  }

  /* Empty state */
  .empty-state {
    text-align: center;
    padding: 2rem;
    color: #a0aec0;
  }

  .empty-icon {
    font-size: 2.5rem;
    display: block;
    margin-bottom: 1rem;
  }

  /* Participants list */
  .participants-list {
    max-height: 300px;
    overflow-y: auto;
    border-radius: 8px;
    background: white;
    border: 1px solid #e2e8f0;
  }

  .participant-item {
    display: flex;
    align-items: center;
    padding: 1rem;
    border-bottom: 1px solid #edf2f7;
    transition: background-color 0.2s ease;
  }

  .participant-item:last-child {
    border-bottom: none;
  }

  .participant-item:hover {
    background-color: #f7fafc;
  }

  .participant-number {
    background: #edf2f7;
    color: #4a5568;
    font-weight: bold;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.85rem;
    margin-right: 1rem;
  }

  .participant-info {
    flex: 1;
  }

  .participant-name {
    font-weight: 600;
    color: #2d3748;
    margin-bottom: 0.25rem;
  }

  .participant-email {
    font-size: 0.85rem;
    color: #718096;
    font-family: 'Courier New', monospace;
  }

  .participant-status {
    margin-left: 1rem;
  }

  .assigned-badge {
    background: #c6f6d5;
    color: #22543d;
    font-size: 0.75rem;
    padding: 0.3rem 0.6rem;
    border-radius: 12px;
    font-weight: 500;
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
  }

  .pending-badge {
    background: #fed7d7;
    color: #9b2c2c;
    font-size: 0.75rem;
    padding: 0.3rem 0.6rem;
    border-radius: 12px;
    font-weight: 500;
    display: inline-flex;
    align-items: center;
    gap: 0.25rem;
  }

  /* Participants summary */
  .participants-summary {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1rem;
    margin-top: 1.5rem;
    padding-top: 1.5rem;
    border-top: 1px dashed #e2e8f0;
  }

  .summary-item {
    text-align: center;
  }

  .summary-label {
    display: block;
    font-size: 0.85rem;
    color: #718096;
    margin-bottom: 0.25rem;
  }

  .summary-value {
    display: block;
    font-weight: bold;
    color: #2d3748;
    font-size: 1.1rem;
  }

  .requirements {
    background: #f7fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 1rem;
    margin: 1.5rem 0;
  }

  .requirements h4 {
    color: #2d5c2d;
    margin-top: 0;
    margin-bottom: 0.5rem;
  }

  .requirements ul {
    margin: 0.5rem 0;
    padding-left: 1.25rem;
    color: #4a5568;
  }

  .requirements li {
    margin-bottom: 0.25rem;
  }

  .note {
    font-size: 0.85rem;
    color: #718096;
    font-style: italic;
    margin-top: 0.5rem;
  }

  .error-message {
    background: #fed7d7;
    border: 1px solid #fc8181;
    color: #9b2c2c;
    padding: 0.75rem;
    border-radius: 8px;
    margin-bottom: 1rem;
    text-align: center;
  }

  .success-message {
    text-align: center;
    padding: 1rem;
  }

  .success-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
    color: #2d5c2d;
  }

  .success-message h3 {
    color: #2d5c2d;
    margin-bottom: 0.5rem;
  }

  /* Action buttons */
  .action-buttons {
    display: flex;
    gap: 1rem;
    margin-top: 1.5rem;
  }

  .assign-button {
    flex: 2;
    padding: 0.875rem;
    background: #c53030;
    color: white;
    border: none;
    border-radius: 8px;
    font-weight: bold;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    transition: all 0.2s ease;
  }

  .assign-button:hover:not(:disabled) {
    background: #9b2c2c;
    transform: translateY(-1px);
  }

  .assign-button:disabled {
    background: #cbd5e0;
    cursor: not-allowed;
    transform: none;
  }

  .cancel-button {
    flex: 1;
    padding: 0.875rem;
    background: #e2e8f0;
    color: #4a5568;
    border: none;
    border-radius: 8px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .cancel-button:hover:not(:disabled) {
    background: #cbd5e0;
  }

  .cancel-button:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top: 2px solid white;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
</style>