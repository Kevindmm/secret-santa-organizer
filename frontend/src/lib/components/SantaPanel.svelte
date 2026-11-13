<!-- frontend/src/lib/components/SantaPanel.svelte -->
<script>
  import { createEventDispatcher, onDestroy } from 'svelte';
  import {
    AlertCircle,
    Check,
    Clock,
    Info,
    KeyRound,
    Mail,
    Shuffle,
    Sparkles,
    UserX,
    Users,
    X
  } from 'lucide-svelte';
  import { API_URL } from '$lib/config';

  const dispatch = createEventDispatcher();

  /**
   * @typedef {{ id?: number, name?: string, email?: string, assigned?: boolean }} Participant
   */

  // Admin panel state
  let showAdminPanel = false;
  let adminGameCode = '';
  let adminLoading = false;
  /** @type {string | null} */
  let adminError = null;
  let adminSuccess = false;
  /** 'unknown' when the server did not report delivery counts (older deploy). */
  /** @type {'unknown' | 'all' | 'partial' | 'none'} */
  let emailStatus = 'unknown';
  /** Number of assignment emails the backend could actually hand to Brevo. */
  let emailsDispatched = 0;
  /** Number of participants in the game that was drawn. */
  let emailRecipients = 0;
  /** @type {Participant[]} */
  let participants = [];
  let loadingParticipants = false;
  /** @type {ReturnType<typeof setTimeout> | undefined} */
  let closeTimer;
  /** @type {AbortController | undefined} */
  let participantsController;
  let participantsRequestId = 0;

  // Fetch participants for a game
  /** @param {string} gameCode */
  async function fetchParticipants(gameCode) {
    if (!gameCode.trim()) return;

    const requestId = ++participantsRequestId;
    participantsController?.abort();
    const controller = new AbortController();
    participantsController = controller;
    const timeoutId = setTimeout(() => controller.abort(), 12000);

    loadingParticipants = true;

    try {
      const response = await fetch(
        `${API_URL}/api/games/${gameCode.toUpperCase()}/participants`,
        { signal: controller.signal }
      );

      if (requestId !== participantsRequestId) return;

      if (!response.ok) {
        participants = [];
        return;
      }

      const data = await response.json();
      if (requestId !== participantsRequestId) return;
      participants = data.participants || data || [];
    } catch (error) {
      if (requestId === participantsRequestId) {
        participants = [];
      }
    } finally {
      clearTimeout(timeoutId);
      if (requestId === participantsRequestId) {
        loadingParticipants = false;
      }
    }
  }

  // Watch for game code changes
  $: if (adminGameCode && adminGameCode.length >= 3) {
    fetchParticipants(adminGameCode);
  }

  // Format email for display (partially masked)
  /** @param {string | undefined} email */
  function formatEmail(email) {
    // The backend already returns the masked email (e.g. an***@domain.com).
    return email || '';
  }

  /** @param {string | undefined} name */
  function initial(name) {
    return (name || '?').trim().charAt(0).toUpperCase() || '?';
  }

  // Assign secret santas
  async function assignSecretSantas() {
    if (!adminGameCode.trim()) {
      adminError = 'Ingresa el código del juego';
      return;
    }

    adminLoading = true;
    adminError = null;
    emailStatus = 'unknown';
    emailsDispatched = 0;
    emailRecipients = 0;

    const gameCode = adminGameCode.toUpperCase();

    try {
      const response = await fetch(`${API_URL}/api/games/${gameCode}/assign`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) {
        if (response.status === 404) {
          adminError = 'Juego no encontrado';
        } else if (response.status === 400) {
          adminError = 'Se necesitan al menos 3 participantes';
        } else if (response.status === 409) {
          adminError = 'El sorteo ya se hizo para este juego';
        } else if (response.status === 429) {
          adminError = 'Demasiados intentos. Espera un minuto antes de volver a sortear.';
        } else {
          adminError = 'Error al asignar amigos secretos';
        }
        return;
      }

      const result = await response.json().catch(() => null);
      if (result && Number.isFinite(result.emailsDispatched) && Number.isFinite(result.participants)) {
        emailsDispatched = result.emailsDispatched;
        emailRecipients = result.participants;
        emailStatus =
          emailsDispatched === emailRecipients
            ? 'all'
            : emailsDispatched > 0
              ? 'partial'
              : 'none';
      }

      adminSuccess = true;

      closeTimer = setTimeout(() => {
        dispatch('assigned', { gameCode });
        showAdminPanel = false;
        adminSuccess = false;
        adminGameCode = '';
        participants = [];
      }, 10000);
    } catch {
      adminError = 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
    } finally {
      adminLoading = false;
    }
  }

  // Close panel
  function closePanel() {
    participantsController?.abort();
    clearTimeout(closeTimer);
    showAdminPanel = false;
    adminError = null;
    adminSuccess = false;
    participants = [];
    adminGameCode = '';
    loadingParticipants = false;
  }

  /** @param {KeyboardEvent} event */
  function handleKeydown(event) {
    if (event.key === 'Escape' && showAdminPanel) {
      closePanel();
    }
  }

  /**
   * The header has backdrop-filter, which creates a containing block for
   * position: fixed. Move the modal to the body so it anchors to the viewport.
   * @param {HTMLElement} node
   */
  function portal(node) {
    document.body.appendChild(node);

    return {
      destroy() {
        node.remove();
      }
    };
  }

  // Lock the page behind the sheet. The class on <html> also pauses the
  // ambient snow animation while the panel is open.
  $: if (typeof document !== 'undefined') {
    document.documentElement.classList.toggle('modal-open', showAdminPanel);
    document.body.style.overflow = showAdminPanel ? 'hidden' : '';
  }

  onDestroy(() => {
    participantsController?.abort();
    clearTimeout(closeTimer);
    if (typeof document !== 'undefined') {
      document.documentElement.classList.remove('modal-open');
      document.body.style.overflow = '';
    }
  });
</script>

<svelte:window on:keydown={handleKeydown} />

<!-- Admin trigger -->
<button
  class="admin-trigger"
  type="button"
  on:click={() => (showAdminPanel = true)}
  title="Panel de Santa - Asignar amigos secretos"
  aria-haspopup="dialog"
  aria-label="Abrir Panel Santa"
>
  <Sparkles size={17} strokeWidth={1.9} aria-hidden="true" />
  <span class="trigger-label">Panel Santa</span>
</button>

<!-- Admin panel modal -->
{#if showAdminPanel}
  <div class="sheet-backdrop" role="presentation" use:portal on:click|self={closePanel}>
    <div
      class="sheet"
      role="dialog"
      aria-modal="true"
      aria-labelledby="santa-title"
    >
      <div class="sheet-handle" aria-hidden="true"></div>

      <div class="sheet-head">
        <div class="sheet-title">
          <p class="eyebrow">Sorteo</p>
          <h2 id="santa-title">Panel de Santa</h2>
          <p class="sheet-subtitle">Asigna los amigos secretos de un intercambio.</p>
        </div>

        <button
          class="btn btn-ghost btn-icon close-button"
          type="button"
          on:click={closePanel}
          aria-label="Cerrar panel"
        >
          <X size={20} aria-hidden="true" />
        </button>
      </div>

      {#if adminSuccess}
        <div class="success-message" role="status">
          <span class="success-mark" aria-hidden="true">
            <Check size={30} strokeWidth={2.2} />
          </span>
          <h3>¡Sorteo completado!</h3>
          <p class="success-lede">
            Cada participante ya tiene su amigo secreto asignado.
          </p>

          <div class="mail-notice" class:is-warning={emailStatus === 'partial' || emailStatus === 'none'}>
            <Mail size={19} aria-hidden="true" />
            <div>
              {#if emailStatus === 'partial'}
                <strong>Emails enviados a {emailsDispatched} de {emailRecipients}</strong>
                <span>
                  Algunos participantes no han recibido el aviso. Revisa la
                  configuración de correo del servidor.
                </span>
              {:else if emailStatus === 'none'}
                <strong>No se han enviado los emails</strong>
                <span>
                  El sorteo está hecho, pero el servidor no ha podido enviar los
                  avisos. Revisa la configuración de correo antes de repetirlo.
                </span>
              {:else if emailStatus === 'all'}
                <strong>Emails enviados</strong>
                <span>
                  Los avisos automáticos se han enviado correctamente a los
                  {emailRecipients} participantes.
                </span>
              {:else}
                <strong>Emails enviados</strong>
                <span>
                  Los avisos automáticos se envían a cada participante por correo.
                </span>
              {/if}
            </div>
          </div>

          <div class="sheet-progress" aria-hidden="true"></div>
          <p class="note">Este panel se cerrará automáticamente en unos segundos.</p>

          <button type="button" class="btn btn-secondary btn-block" on:click={closePanel}>
            Cerrar panel
          </button>
        </div>
      {:else}
        <div class="admin-form">
          {#if adminError}
            <div class="alert alert-error admin-error" role="alert">
              <AlertCircle size={17} aria-hidden="true" />
              <span>{adminError}</span>
            </div>
          {/if}

          <div class="field">
            <label class="field-label" for="adminGameCode">Código del juego</label>
            <div class="input-wrap">
              <KeyRound class="input-icon" size={18} aria-hidden="true" />
              <input
                id="adminGameCode"
                class="input code-input"
                type="text"
                bind:value={adminGameCode}
                placeholder="Ej. X8B3K9"
                required
                maxlength="10"
                autocomplete="off"
                autocapitalize="characters"
                spellcheck="false"
                disabled={adminLoading}
              />
            </div>
            <p class="field-hint">
              Escríbelo tal como lo compartiste con el grupo.
            </p>
          </div>

          <!-- Participants list -->
          {#if adminGameCode && adminGameCode.length >= 3}
            <section class="participants-section" aria-live="polite">
              <div class="section-head">
                <h3>
                  <Users size={17} aria-hidden="true" />
                  Participantes
                </h3>

                {#if loadingParticipants}
                  <span class="mini-spinner" aria-label="Cargando"></span>
                {:else}
                  <span class="chip chip-gold count-chip">{participants.length}</span>
                {/if}
              </div>

              {#if loadingParticipants}
                <div class="loading-state">
                  <span class="spinner" aria-hidden="true"></span>
                  <p>Buscando participantes…</p>
                </div>
              {:else if participants.length === 0}
                <div class="empty-state">
                  <UserX size={24} aria-hidden="true" />
                  <p>No hay participantes todavía</p>
                  <span>Comparte el código para que se apunten.</span>
                </div>
              {:else}
                <ul class="participants-list">
                  {#each participants as participant, index (participant.email || index)}
                    <li class="participant-item">
                      <span class="participant-avatar" aria-hidden="true">
                        {initial(participant.name)}
                      </span>

                      <span class="participant-info">
                        <span class="participant-name">
                          {participant.name || 'Sin nombre'}
                        </span>
                        <span class="participant-email">
                          {formatEmail(participant.email)}
                        </span>
                      </span>

                      {#if participant.assigned}
                        <span class="status-pill assigned">
                          <Check size={13} aria-hidden="true" />
                          Asignado
                        </span>
                      {:else}
                        <span class="status-pill pending">
                          <Clock size={13} aria-hidden="true" />
                          Pendiente
                        </span>
                      {/if}
                    </li>
                  {/each}
                </ul>

                <div class="participants-summary">
                  <div>
                    <span>Total</span>
                    <strong>{participants.length}</strong>
                  </div>
                  <div>
                    <span>Asignados</span>
                    <strong>{participants.filter((p) => p.assigned).length}</strong>
                  </div>
                  <div>
                    <span>Pendientes</span>
                    <strong>{participants.filter((p) => !p.assigned).length}</strong>
                  </div>
                </div>
              {/if}
            </section>
          {/if}

          <div class="requirements">
            <div class="requirements-head">
              <Info size={16} aria-hidden="true" />
              <h4>Antes de hacer el sorteo</h4>
            </div>
            <ul>
              <li><Check size={14} aria-hidden="true" /> Al menos 3 participantes únicos</li>
              <li><Check size={14} aria-hidden="true" /> El juego no debe estar asignado</li>
              <li><Check size={14} aria-hidden="true" /> Todos necesitan un email válido</li>
            </ul>
            <p class="note">
              Al hacer el sorteo, cada participante recibe su asignación por email
              automáticamente.
            </p>
          </div>

          <div class="action-buttons">
            <button
              type="button"
              class="btn btn-primary btn-block"
              on:click={assignSecretSantas}
              disabled={adminLoading || !adminGameCode.trim() || participants.length < 3}
            >
              {#if adminLoading}
                <span class="spinner" aria-hidden="true"></span>
                <span>Sorteando…</span>
              {:else}
                <Shuffle size={18} aria-hidden="true" />
                <span>Hacer el sorteo</span>
              {/if}
            </button>

            <button
              type="button"
              class="btn btn-secondary btn-block"
              on:click={closePanel}
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
  /* Trigger ------------------------------------------------- */

  .admin-trigger {
    display: inline-flex;
    flex: none;
    align-items: center;
    justify-content: center;
    gap: 0.4rem;
    min-height: 48px;
    padding: 0.5rem 0.8rem;
    color: var(--text-soft);
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid var(--line);
    border-radius: 14px;
    font-size: 0.8rem;
    font-weight: 600;
    white-space: nowrap;
    transition:
      color 0.18s ease,
      background-color 0.18s ease,
      border-color 0.18s ease,
      transform 0.18s var(--ease-out);
  }

  .admin-trigger :global(svg) {
    color: var(--gold);
  }

  .admin-trigger:active {
    transform: scale(0.97);
  }

  @media (hover: hover) {
    .admin-trigger:hover {
      color: var(--text);
      background: rgba(255, 255, 255, 0.1);
      border-color: var(--line-strong);
    }
  }

  /* Sheet --------------------------------------------------- */

  .sheet-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 1rem;
  }

  .sheet-title {
    min-width: 0;
  }

  .sheet-title h2 {
    margin: 0.25rem 0 0.2rem;
    font-size: 1.42rem;
    font-weight: 750;
    line-height: 1.15;
    letter-spacing: -0.03em;
  }

  .sheet-subtitle {
    margin: 0;
    color: var(--muted);
    font-size: 0.81rem;
    line-height: 1.4;
  }

  .close-button {
    flex: none;
    margin-top: -0.35rem;
    margin-right: -0.35rem;
  }

  .admin-form {
    display: flex;
    flex-direction: column;
    gap: 1.1rem;
  }

  .admin-error {
    margin-bottom: -0.1rem;
  }

  .code-input {
    font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
    font-weight: 600;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }

  /* Participantes ------------------------------------------- */

  .participants-section {
    padding: 0.85rem;
    background: rgba(255, 255, 255, 0.035);
    border: 1px solid rgba(255, 255, 255, 0.075);
    border-radius: var(--radius-lg);
  }

  .section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    margin-bottom: 0.75rem;
  }

  .section-head h3 {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin: 0;
    color: var(--text);
    font-size: 0.88rem;
    font-weight: 650;
  }

  .section-head h3 :global(svg) {
    color: var(--gold);
  }

  .count-chip {
    min-height: 28px;
    padding: 0.15rem 0.6rem;
    font-size: 0.72rem;
    font-variant-numeric: tabular-nums;
  }

  .mini-spinner {
    width: 16px;
    height: 16px;
    border: 2px solid rgba(255, 255, 255, 0.16);
    border-top-color: var(--gold);
    border-radius: 50%;
    animation: spin 0.7s linear infinite;
  }

  .loading-state {
    padding: 1.4rem 0.5rem;
    color: var(--muted);
    text-align: center;
  }

  .loading-state .spinner {
    margin: 0 auto 0.6rem;
    border-color: rgba(255, 255, 255, 0.14);
    border-top-color: var(--gold);
  }

  .loading-state p {
    margin: 0;
    font-size: 0.8rem;
  }

  .empty-state {
    padding: 1.3rem 0.5rem;
    color: var(--muted);
    text-align: center;
  }

  .empty-state > :global(svg) {
    margin: 0 auto 0.5rem;
    color: var(--muted);
    opacity: 0.8;
  }

  .empty-state p {
    margin: 0 0 0.15rem;
    color: var(--text-soft);
    font-size: 0.84rem;
    font-weight: 600;
  }

  .empty-state span {
    font-size: 0.75rem;
  }

  .participants-list {
    display: grid;
    gap: 0.4rem;
    max-height: 270px;
    padding: 0;
    margin: 0;
    overflow-y: auto;
    overscroll-behavior: contain;
    list-style: none;
  }

  .participant-item {
    display: flex;
    align-items: center;
    gap: 0.6rem;
    padding: 0.5rem 0.6rem;
    background: rgba(0, 0, 0, 0.18);
    border: 1px solid rgba(255, 255, 255, 0.055);
    border-radius: 14px;
  }

  .participant-avatar {
    display: grid;
    flex: none;
    place-items: center;
    width: 34px;
    height: 34px;
    color: #d9f4e5;
    background: linear-gradient(145deg, rgba(45, 122, 85, 0.75), rgba(20, 73, 47, 0.9));
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    font-size: 0.8rem;
    font-weight: 700;
  }

  .participant-info {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
  }

  .participant-name {
    overflow: hidden;
    color: var(--text);
    font-size: 0.82rem;
    font-weight: 600;
    line-height: 1.3;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .participant-email {
    overflow: hidden;
    color: var(--muted);
    font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
    font-size: 0.67rem;
    line-height: 1.35;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .participants-summary {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 0.4rem;
    padding-top: 0.75rem;
    margin-top: 0.75rem;
    border-top: 1px dashed rgba(255, 255, 255, 0.09);
  }

  .participants-summary div {
    text-align: center;
  }

  .participants-summary span {
    display: block;
    color: var(--muted);
    font-size: 0.62rem;
    font-weight: 600;
    letter-spacing: 0.05em;
    text-transform: uppercase;
  }

  .participants-summary strong {
    display: block;
    color: var(--text);
    font-size: 0.95rem;
    font-variant-numeric: tabular-nums;
  }

  /* Requisitos y acciones ----------------------------------- */

  .requirements {
    padding: 0.85rem 0.9rem;
    background: rgba(255, 255, 255, 0.028);
    border: 1px solid rgba(255, 255, 255, 0.07);
    border-radius: 16px;
  }

  .requirements-head {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin-bottom: 0.55rem;
  }

  .requirements-head :global(svg) {
    color: var(--gold);
  }

  .requirements h4 {
    margin: 0;
    color: var(--text-soft);
    font-size: 0.73rem;
    font-weight: 700;
    letter-spacing: 0.07em;
    text-transform: uppercase;
  }

  .requirements ul {
    display: grid;
    gap: 0.35rem;
    padding: 0;
    margin: 0;
    list-style: none;
  }

  .requirements li {
    display: flex;
    align-items: flex-start;
    gap: 0.4rem;
    color: var(--muted);
    font-size: 0.76rem;
    line-height: 1.4;
  }

  .requirements li :global(svg) {
    flex: none;
    margin-top: 0.12rem;
    color: var(--success);
  }

  .note {
    margin: 0.6rem 0 0;
    color: var(--muted);
    font-size: 0.72rem;
    line-height: 1.45;
  }

  .action-buttons {
    display: grid;
    gap: 0.6rem;
  }

  /* Éxito --------------------------------------------------- */

  .success-message {
    padding: 0.4rem 0 0;
    text-align: center;
  }

  .success-mark {
    display: grid;
    place-items: center;
    width: 62px;
    height: 62px;
    margin: 0 auto 0.9rem;
    color: #bdf6da;
    background:
      radial-gradient(circle at 30% 25%, rgba(255, 255, 255, 0.22), transparent 55%),
      linear-gradient(145deg, rgba(102, 199, 154, 0.3), rgba(45, 122, 85, 0.18));
    border: 1px solid rgba(102, 199, 154, 0.35);
    border-radius: 22px;
    box-shadow: 0 16px 36px -18px rgba(102, 199, 154, 0.65);
    animation: pop-in 0.42s var(--ease-out) both;
  }

  .success-message h3 {
    margin: 0 0 0.35rem;
    font-size: 1.35rem;
    font-weight: 750;
    letter-spacing: -0.025em;
  }

  .success-lede {
    max-width: 38ch;
    margin: 0 auto;
    color: var(--text-soft);
    font-size: 0.84rem;
    line-height: 1.5;
  }

  .mail-notice {
    display: flex;
    align-items: flex-start;
    gap: 0.7rem;
    padding: 0.85rem 0.9rem;
    margin: 1.1rem 0 0;
    text-align: left;
    background: rgba(102, 199, 154, 0.09);
    border: 1px solid rgba(102, 199, 154, 0.24);
    border-radius: 16px;
  }

  .mail-notice > :global(svg) {
    flex: none;
    margin-top: 0.1rem;
    color: var(--success);
  }

  .mail-notice div {
    display: flex;
    flex-direction: column;
    gap: 0.1rem;
  }

  .mail-notice strong {
    color: #bdf6da;
    font-size: 0.67rem;
    font-weight: 700;
    letter-spacing: 0.09em;
    text-transform: uppercase;
  }

  .mail-notice span {
    color: var(--text-soft);
    font-size: 0.78rem;
    line-height: 1.45;
  }

  .mail-notice.is-warning {
    background: rgba(240, 122, 116, 0.08);
    border-color: rgba(240, 122, 116, 0.26);
  }

  .mail-notice.is-warning > :global(svg) {
    color: var(--danger);
  }

  .mail-notice.is-warning strong {
    color: #ffc9c5;
  }

  .sheet-progress {
    height: 3px;
    margin: 1.2rem 0 0.6rem;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.08);
    border-radius: 999px;
  }

  .sheet-progress::after {
    display: block;
    width: 100%;
    height: 100%;
    content: "";
    background: linear-gradient(90deg, var(--gold), var(--success));
    border-radius: inherit;
    transform-origin: left center;
    animation: progress 10s linear forwards;
  }

  .success-message .btn {
    margin-top: 0.9rem;
  }

  @media (min-width: 480px) {
    .action-buttons {
      grid-template-columns: 1.6fr 1fr;
    }
  }

  @media (max-width: 420px) {
    .trigger-label {
      display: none;
    }

    .admin-trigger {
      width: 48px;
      padding: 0;
    }
  }
</style>
