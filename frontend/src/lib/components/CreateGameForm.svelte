<!-- frontend/src/lib/components/CreateGameForm.svelte -->
<script>
  import { onDestroy, onMount } from 'svelte';
  import {
    AlertCircle,
    ArrowRight,
    Calendar,
    Check,
    Copy,
    Gift,
    Mail,
    PartyPopper,
    PencilLine,
    Plus,
    Wallet
  } from 'lucide-svelte';

  let gameName = '';
  let maxPrice = 10;
  let exchangeDate = '';
  let loading = false;
  /** @type {string | null} */
  let error = null;
  /** @type {{ gameId: string, joinUrl?: string } | null} */
  let successData = null;

  let copied = false;
  /** @type {ReturnType<typeof setTimeout> | undefined} */
  let copyTimer;

  $: rangeProgress = (maxPrice / 50) * 100;

  function initDate() {
    const today = new Date();
    const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 10);
    exchangeDate = nextMonth.toISOString().split('T')[0];
  }

  onMount(initDate);

  onDestroy(() => clearTimeout(copyTimer));

  async function createGame() {
    const API_URL = 'https://secret-santa-organizerv3.onrender.com';

    if (!gameName.trim()) {
      error = 'Por favor ingresa un nombre para el juego';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await fetch(`${API_URL}/api/games`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: gameName,
          maxPrice: maxPrice,
          exchangeDate: exchangeDate
        })
      });

      if (!response.ok) {
        const err = await response.text();
        throw new Error(`API Error (${response.status}): ${err}`);
      }

      successData = await response.json();
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      error =
        message ||
        'Error al crear el juego. Verifica que el backend esté ejecutándose.';
    } finally {
      loading = false;
    }
  }

  async function copyJoinLink() {
    if (!successData?.gameId) return;

    const gameCode = successData.gameId;

    try {
      await navigator.clipboard.writeText(gameCode);
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = gameCode;
      textArea.setAttribute('readonly', '');
      textArea.style.position = 'fixed';
      textArea.style.opacity = '0';
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
    }

    copied = true;
    clearTimeout(copyTimer);
    copyTimer = setTimeout(() => {
      copied = false;
    }, 2200);
  }

  function createAnother() {
    clearTimeout(copyTimer);
    copied = false;
    successData = null;
    gameName = '';
    maxPrice = 10;
    initDate();
  }
</script>

<div class="create-game-form glass-card">
  {#if successData}
    <div class="success-card pop-in">
      <div class="success-hero">
        <span class="success-mark" aria-hidden="true">
          <PartyPopper size={28} strokeWidth={1.8} />
        </span>
        <p class="eyebrow">Todo listo</p>
        <h2>¡{gameName} ya está en marcha!</h2>
        <p class="success-lede">
          Comparte el código con tu grupo. Cuando todos se hayan unido, harás el
          sorteo desde el Panel Santa.
        </p>
      </div>

      <div class="code-card">
        <span class="code-label">Código para compartir</span>
        <strong class="code-value">{successData.gameId}</strong>
        <button
          type="button"
          class="btn btn-secondary copy-btn"
          class:is-copied={copied}
          on:click={copyJoinLink}
        >
          {#if copied}
            <Check size={17} aria-hidden="true" />
            <span>Copiado</span>
          {:else}
            <Copy size={17} aria-hidden="true" />
            <span>Copiar código</span>
          {/if}
        </button>
      </div>

      <ul class="detail-list">
        <li>
          <span class="detail-icon" aria-hidden="true">
            <PencilLine size={16} />
          </span>
          <span class="detail-copy">
            <span>Nombre</span>
            <strong>{gameName}</strong>
          </span>
        </li>
        <li>
          <span class="detail-icon" aria-hidden="true">
            <Wallet size={16} />
          </span>
          <span class="detail-copy">
            <span>Presupuesto máximo</span>
            <strong>{maxPrice} €</strong>
          </span>
        </li>
        <li>
          <span class="detail-icon" aria-hidden="true">
            <Calendar size={16} />
          </span>
          <span class="detail-copy">
            <span>Fecha del intercambio</span>
            <strong>
              {new Date(exchangeDate).toLocaleDateString('es-ES', {
                day: 'numeric',
                month: 'long',
                year: 'numeric'
              })}
            </strong>
          </span>
        </li>
      </ul>

      <div class="next-step">
        <Mail size={18} aria-hidden="true" />
        <p>
          <strong>Siguiente paso</strong>
          Abre el Panel Santa y pulsa “Hacer el sorteo”. Los emails se envían
          automáticamente a cada participante.
        </p>
      </div>

      <button type="button" class="btn btn-secondary btn-block secondary-btn" on:click={createAnother}>
        <Plus size={17} aria-hidden="true" />
        Crear otro intercambio
      </button>
    </div>
  {:else}
    <div class="form-header">
      <span class="form-icon" aria-hidden="true">
        <Gift size={21} strokeWidth={1.8} />
      </span>
      <div>
        <h2>Crear un intercambio</h2>
        <p>Define los detalles y comparte el código.</p>
      </div>
    </div>

    {#if error}
      <div class="alert alert-error form-error" role="alert">
        <AlertCircle size={17} aria-hidden="true" />
        <span>{error}</span>
      </div>
    {/if}

    <form class="form-body" on:submit|preventDefault={createGame}>
      <div class="field">
        <label class="field-label" for="gameName">Nombre del intercambio</label>
        <div class="input-wrap">
          <PencilLine class="input-icon" size={18} aria-hidden="true" />
          <input
            id="gameName"
            class="input"
            type="text"
            bind:value={gameName}
            placeholder="Ej. Navidad en casa"
            required
            maxlength="50"
            autocomplete="off"
            disabled={loading}
          />
        </div>
        <p class="field-hint">Algo que reconozca todo el grupo.</p>
      </div>

      <div class="field">
        <label class="field-label" for="maxPrice">
          <span>Límite de presupuesto</span>
          <span class="budget-pill">{maxPrice} €</span>
        </label>
        <input
          id="maxPrice"
          class="range"
          type="range"
          bind:value={maxPrice}
          min="0"
          max="50"
          step="5"
          disabled={loading}
          style="--range-progress: {rangeProgress}%"
        />
        <div class="range-scale" aria-hidden="true">
          <span>0 €</span>
          <span>25 €</span>
          <span>50 €</span>
        </div>
        <p class="field-hint">Un límite claro evita malentendidos.</p>
      </div>

      <div class="field">
        <label class="field-label" for="exchangeDate">Fecha del intercambio</label>
        <div class="input-wrap">
          <Calendar class="input-icon" size={18} aria-hidden="true" />
          <input
            id="exchangeDate"
            class="input"
            type="date"
            bind:value={exchangeDate}
            required
            min={new Date().toISOString().split('T')[0]}
            disabled={loading}
          />
        </div>
      </div>

      <button
        type="submit"
        class="btn btn-primary btn-block submit-btn"
        disabled={loading || !gameName.trim()}
      >
        {#if loading}
          <span class="spinner" aria-hidden="true"></span>
          <span>Creando intercambio…</span>
        {:else}
          <span>Crear intercambio</span>
          <ArrowRight size={18} aria-hidden="true" />
        {/if}
      </button>

      <p class="form-note">Recibirás un código para compartir con el grupo.</p>
    </form>
  {/if}
</div>

<style>
  .create-game-form {
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

  .budget-pill {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0.2rem 0.65rem;
    color: #f4dfa3;
    background: var(--gold-soft);
    border: 1px solid rgba(233, 203, 123, 0.28);
    border-radius: 999px;
    font-size: 0.82rem;
    font-weight: 700;
    white-space: nowrap;
  }

  .range {
    width: 100%;
    height: 44px;
    padding: 0;
    margin: 0;
    background: transparent;
    cursor: pointer;
    -webkit-appearance: none;
    appearance: none;
  }

  .range::-webkit-slider-runnable-track {
    height: 8px;
    background: linear-gradient(
      to right,
      var(--red) 0%,
      var(--red) var(--range-progress),
      rgba(255, 255, 255, 0.12) var(--range-progress),
      rgba(255, 255, 255, 0.12) 100%
    );
    border-radius: 999px;
  }

  .range::-webkit-slider-thumb {
    width: 28px;
    height: 28px;
    margin-top: -10px;
    background: #fff;
    border: 4px solid var(--red);
    border-radius: 50%;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.42);
    transition: transform 0.16s ease, box-shadow 0.16s ease;
    -webkit-appearance: none;
    appearance: none;
  }

  .range:active::-webkit-slider-thumb {
    transform: scale(1.08);
    box-shadow: 0 6px 18px rgba(227, 93, 87, 0.45);
  }

  .range::-moz-range-track {
    height: 8px;
    background: rgba(255, 255, 255, 0.12);
    border-radius: 999px;
  }

  .range::-moz-range-progress {
    height: 8px;
    background: var(--red);
    border-radius: 999px;
  }

  .range::-moz-range-thumb {
    width: 22px;
    height: 22px;
    background: #fff;
    border: 4px solid var(--red);
    border-radius: 50%;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.42);
  }

  .range:focus-visible {
    outline: 2px solid var(--gold);
    outline-offset: 4px;
    border-radius: 999px;
  }

  .range-scale {
    display: flex;
    justify-content: space-between;
    margin-top: -0.25rem;
    color: var(--muted);
    font-size: 0.7rem;
    font-weight: 600;
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

  /* Estado de éxito ---------------------------------------- */

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

  .code-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.35rem;
    padding: 1.15rem 1rem;
    margin: 1.35rem 0 1.1rem;
    text-align: center;
    background: rgba(0, 0, 0, 0.24);
    border: 1px dashed rgba(233, 203, 123, 0.34);
    border-radius: var(--radius-lg);
  }

  .code-label {
    color: var(--muted);
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.14em;
    text-transform: uppercase;
  }

  .code-value {
    max-width: 100%;
    color: var(--gold);
    font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
    font-size: clamp(1.6rem, 9vw, 2.15rem);
    font-weight: 700;
    line-height: 1.2;
    letter-spacing: 0.16em;
    overflow-wrap: anywhere;
  }

  .copy-btn {
    min-height: 48px;
    margin-top: 0.55rem;
  }

  .copy-btn.is-copied {
    color: #bdf6da;
    background: rgba(102, 199, 154, 0.14);
    border-color: rgba(102, 199, 154, 0.32);
  }

  .detail-list {
    display: grid;
    gap: 0.5rem;
    padding: 0;
    margin: 0;
    list-style: none;
    text-align: left;
  }

  .detail-list li {
    display: flex;
    align-items: center;
    gap: 0.7rem;
    padding: 0.65rem 0.75rem;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid rgba(255, 255, 255, 0.07);
    border-radius: 14px;
  }

  .detail-icon {
    display: grid;
    flex: none;
    place-items: center;
    width: 34px;
    height: 34px;
    color: var(--gold);
    background: rgba(233, 203, 123, 0.09);
    border-radius: 10px;
  }

  .detail-copy {
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  .detail-copy span {
    color: var(--muted);
    font-size: 0.66rem;
    font-weight: 700;
    letter-spacing: 0.1em;
    text-transform: uppercase;
  }

  .detail-copy strong {
    overflow-wrap: anywhere;
    color: var(--text);
    font-size: 0.88rem;
    font-weight: 600;
  }

  .next-step {
    display: flex;
    align-items: flex-start;
    gap: 0.7rem;
    padding: 0.85rem 0.9rem;
    margin: 1.1rem 0;
    color: var(--text-soft);
    background: rgba(233, 203, 123, 0.075);
    border: 1px solid rgba(233, 203, 123, 0.18);
    border-radius: 16px;
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
    .create-game-form {
      padding: 1.6rem;
    }

    .form-body {
      gap: 1.35rem;
    }
  }
</style>
