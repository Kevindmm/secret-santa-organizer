<!-- frontend/src/lib/components/JoinGameForm.svelte -->
<script>
  import {
    AlertCircle,
    ArrowRight,
    Check,
    Gift,
    KeyRound,
    Mail,
    Sparkles,
    User,
    UserPlus
  } from 'lucide-svelte';
  import { API_URL } from '$lib/config';

  // State
  let gameCode = '';
  let participantName = '';
  let participantEmail = '';
  let participantWishlist = '';
  let loading = false;
  /** @type {string | null} */
  let error = null;
  let success = false;

  // Auto-uppercase game code
  $: formattedGameCode = gameCode.toUpperCase();

  // API call to join game
  async function joinGame() {
    // Validation
    if (!gameCode.trim()) {
      error = 'Por favor ingresa el código del juego';
      return;
    }
    if (!participantName.trim()) {
      error = 'Por favor ingresa tu nombre';
      return;
    }
    if (!participantEmail.trim() || !isValidEmail(participantEmail)) {
      error = 'Por favor ingresa un email válido';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await fetch(`${API_URL}/api/games/${formattedGameCode}/participants`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: participantName,
          email: participantEmail,
          wishList: participantWishlist
        })
      });

      if (!response.ok) {
        if (response.status === 404) {
          error = 'Juego no encontrado. Verifica el código.';
        } else if (response.status === 409) {
          error = 'Este email ya está registrado en el juego.';
        } else if (response.status === 429) {
          error = 'Demasiados intentos. Espera un minuto y vuelve a probar.';
        } else {
          error = 'Error al unirse al juego. Verifica los datos.';
        }
        return;
      }

      success = true;
    } catch {
      error = 'No se pudo conectar con el servidor. Inténtalo de nuevo.';
    } finally {
      loading = false;
    }
  }

  // Simple email validation
  /** @param {string} email */
  function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  // Reset form
  function joinAnother() {
    success = false;
    gameCode = '';
    participantName = '';
    participantEmail = '';
    participantWishlist = '';
  }
</script>

<div class="join-game-form glass-card">
  {#if success}
    <div class="success-card pop-in" role="status">
      <div class="success-hero">
        <span class="success-mark" aria-hidden="true">
          <Check size={30} strokeWidth={2.2} />
        </span>
        <p class="eyebrow">Estás dentro</p>
        <h2>¡Bienvenido, {participantName}!</h2>
        <p class="success-lede">
          Tu plaza está confirmada. Solo queda esperar a que el organizador haga
          el sorteo.
        </p>

        <div class="pill-row">
          <span class="chip chip-gold">
            <KeyRound size={14} aria-hidden="true" />
            {formattedGameCode}
          </span>
          <span class="chip name-chip">
            <User size={14} aria-hidden="true" />
            <span class="truncate">{participantName}</span>
          </span>
        </div>
      </div>

      <div class="info-card">
        <h3>
          <Mail size={17} aria-hidden="true" />
          ¿Qué pasa ahora?
        </h3>
        <ol>
          <li>
            <span aria-hidden="true">1</span>
            Espera a que el organizador haga el sorteo.
          </li>
          <li>
            <span aria-hidden="true">2</span>
            Recibirás un email con tu amigo secreto.
          </li>
          <li>
            <span aria-hidden="true">3</span>
            Mantén el secreto hasta el día del intercambio.
          </li>
        </ol>
      </div>

      <div class="next-step">
        <Sparkles size={18} aria-hidden="true" />
        <p>
          <strong>Consejo</strong>
          Añade ideas a tu lista de deseos si aún no lo has hecho: ayudan mucho a
          quien te toque.
        </p>
      </div>

      <button type="button" class="btn btn-secondary btn-block secondary-btn" on:click={joinAnother}>
        <UserPlus size={17} aria-hidden="true" />
        Unirme a otro intercambio
      </button>
    </div>
  {:else}
    <div class="form-header">
      <span class="form-icon" aria-hidden="true">
        <Gift size={21} strokeWidth={1.8} />
      </span>
      <div>
        <h2>Unirme a un intercambio</h2>
        <p>Necesitas el código que te ha pasado el organizador.</p>
      </div>
    </div>

    {#if error}
      <div class="alert alert-error form-error" role="alert">
        <AlertCircle size={17} aria-hidden="true" />
        <span>{error}</span>
      </div>
    {/if}

    <form class="form-body" on:submit|preventDefault={joinGame}>
      <div class="field">
        <label class="field-label" for="gameCode">Código del juego</label>
        <div class="input-wrap">
          <KeyRound class="input-icon" size={18} aria-hidden="true" />
          <input
            id="gameCode"
            class="input code-input"
            type="text"
            bind:value={gameCode}
            placeholder="Ej. X8B3K9"
            required
            maxlength="10"
            autocomplete="off"
            autocapitalize="characters"
            spellcheck="false"
            disabled={loading}
          />
        </div>
        {#if gameCode.trim()}
          <p class="field-hint code-hint">
            Te unirás al código <strong>{formattedGameCode}</strong>
          </p>
        {:else}
          <p class="field-hint">Pídeselo al organizador del intercambio.</p>
        {/if}
      </div>

      <div class="field">
        <label class="field-label" for="participantName">Tu nombre</label>
        <div class="input-wrap">
          <User class="input-icon" size={18} aria-hidden="true" />
          <input
            id="participantName"
            class="input"
            type="text"
            bind:value={participantName}
            placeholder="Tu nombre completo"
            required
            maxlength="50"
            autocomplete="name"
            disabled={loading}
          />
        </div>
      </div>

      <div class="field">
        <label class="field-label" for="participantEmail">Tu email</label>
        <div class="input-wrap">
          <Mail class="input-icon" size={18} aria-hidden="true" />
          <input
            id="participantEmail"
            class="input"
            type="email"
            bind:value={participantEmail}
            placeholder="tucorreo@ejemplo.com"
            required
            autocomplete="email"
            inputmode="email"
            disabled={loading}
          />
        </div>
        <p class="field-hint">Aquí recibirás la asignación de tu amigo secreto.</p>
      </div>

      <div class="field">
        <label class="field-label" for="participantWishlist">
          <span>Lista de deseos</span>
          <span class="optional-tag">Opcional</span>
        </label>
        <textarea
          id="participantWishlist"
          class="input"
          bind:value={participantWishlist}
          placeholder="¿Qué te gustaría recibir? Ej. libros, chocolate, una bufanda…"
          rows="4"
          maxlength="200"
          disabled={loading}
        ></textarea>
        <div class="field-foot">
          <p class="field-hint">Da alguna pista, sin pasarte de presupuesto.</p>
          <span class="char-count">{participantWishlist.length}/200</span>
        </div>
      </div>

      <button
        type="submit"
        class="btn btn-primary btn-block submit-btn"
        disabled={loading ||
          !gameCode.trim() ||
          !participantName.trim() ||
          !participantEmail.trim()}
      >
        {#if loading}
          <span class="spinner" aria-hidden="true"></span>
          <span>Uniéndome…</span>
        {:else}
          <span>Unirme al intercambio</span>
          <ArrowRight size={18} aria-hidden="true" />
        {/if}
      </button>

      <p class="form-note">Tu email solo se usa para enviarte la asignación.</p>
    </form>
  {/if}
</div>

<style>
  .join-game-form {
    padding: 1.15rem;
  }

  .form-header {
    display: flex;
    align-items: center;
    gap: 0.8rem;
    margin-bottom: 1.4rem;
  }

  .form-icon {
    display: grid;
    flex: none;
    place-items: center;
    width: 44px;
    height: 44px;
    color: var(--gold);
    background: var(--gold-soft);
    border: 1px solid rgba(233, 203, 123, 0.22);
    border-radius: 14px;
  }

  .form-header h2 {
    margin: 0 0 0.15rem;
    font-size: 1.24rem;
    font-weight: 700;
    letter-spacing: -0.025em;
  }

  .form-header p {
    margin: 0;
    color: var(--muted);
    font-size: 0.82rem;
    line-height: 1.4;
  }

  .form-error {
    margin-bottom: 1.1rem;
  }

  .form-body {
    display: grid;
    gap: 1.2rem;
  }

  .code-input {
    font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
    font-weight: 600;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }

  .code-hint strong {
    color: var(--gold);
    letter-spacing: 0.08em;
  }

  .optional-tag {
    display: inline-flex;
    align-items: center;
    min-height: 26px;
    padding: 0.12rem 0.55rem;
    color: var(--text-soft);
    background: rgba(255, 255, 255, 0.055);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 999px;
    font-size: 0.66rem;
    font-weight: 600;
    letter-spacing: 0.04em;
    text-transform: uppercase;
  }

  .field-foot {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.75rem;
  }

  .char-count {
    flex: none;
    color: var(--muted);
    font-size: 0.7rem;
    font-variant-numeric: tabular-nums;
  }

  .submit-btn {
    min-height: 56px;
    margin-top: 0.2rem;
    font-size: 1.02rem;
  }

  .form-note {
    margin: -0.35rem 0 0;
    color: var(--muted);
    font-size: 0.76rem;
    text-align: center;
  }

  /* Success state ------------------------------------------ */

  .success-card {
    padding: 0.35rem 0;
  }

  .success-hero {
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
  }

  .success-hero h2 {
    margin: 0.35rem 0 0.5rem;
    font-size: clamp(1.35rem, 5.5vw, 1.7rem);
    font-weight: 750;
    line-height: 1.15;
    letter-spacing: -0.03em;
    text-wrap: balance;
  }

  .success-lede {
    max-width: 40ch;
    margin: 0 auto;
    color: var(--text-soft);
    font-size: 0.86rem;
    line-height: 1.5;
  }

  .pill-row {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.5rem;
    margin-top: 1rem;
  }

  .pill-row .chip :global(svg) {
    color: var(--gold);
  }

  .name-chip {
    max-width: 100%;
  }

  .truncate {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .info-card {
    padding: 1rem;
    margin: 1.25rem 0;
    background: rgba(255, 255, 255, 0.035);
    border: 1px solid rgba(255, 255, 255, 0.075);
    border-radius: var(--radius-lg);
    text-align: left;
  }

  .info-card h3 {
    display: flex;
    align-items: center;
    gap: 0.45rem;
    margin: 0 0 0.8rem;
    color: var(--text);
    font-size: 0.9rem;
    font-weight: 650;
  }

  .info-card h3 :global(svg) {
    color: var(--gold);
  }

  .info-card ol {
    display: grid;
    gap: 0.65rem;
    padding: 0;
    margin: 0;
    list-style: none;
  }

  .info-card li {
    display: flex;
    align-items: flex-start;
    gap: 0.6rem;
    color: var(--text-soft);
    font-size: 0.81rem;
    line-height: 1.45;
  }

  .info-card li > span {
    display: grid;
    flex: none;
    place-items: center;
    width: 24px;
    height: 24px;
    color: var(--gold);
    background: var(--gold-soft);
    border-radius: 50%;
    font-size: 0.68rem;
    font-weight: 700;
  }

  .next-step {
    display: flex;
    align-items: flex-start;
    gap: 0.7rem;
    padding: 0.85rem 0.9rem;
    margin-bottom: 1.1rem;
    color: var(--text-soft);
    background: rgba(233, 203, 123, 0.075);
    border: 1px solid rgba(233, 203, 123, 0.18);
    border-radius: 16px;
    text-align: left;
  }

  .next-step > :global(svg) {
    flex: none;
    margin-top: 0.15rem;
    color: var(--gold);
  }

  .next-step p {
    margin: 0;
    font-size: 0.79rem;
    line-height: 1.5;
  }

  .next-step strong {
    display: block;
    margin-bottom: 0.1rem;
    color: var(--gold);
    font-size: 0.68rem;
    letter-spacing: 0.1em;
    text-transform: uppercase;
  }

  .secondary-btn {
    min-height: 50px;
  }

  @media (min-width: 640px) {
    .join-game-form {
      padding: 1.6rem;
    }

    .form-body {
      gap: 1.35rem;
    }
  }
</style>
