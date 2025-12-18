<!-- frontend/src/lib/components/CreateGameForm.svelte -->
<script>
  import { onMount } from 'svelte';

  // State
  let gameName = '';
  let maxPrice = 5;
  let exchangeDate = '';
  let loading = false;
  let error = null;
  let successData = null; // { gameId, joinUrl }

  // Initialize date to next month 10th
  function initDate() {
    const today = new Date();
    const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 10);
    exchangeDate = nextMonth.toISOString().split('T')[0];
  }

  onMount(initDate);


  // API call
  async function createGame() {
    
    // TEST:
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
      error = err.message || 'Error al crear el juego. Verifica que el backend esté ejecutándose.';
    } finally {
      loading = false;
    }
  }

  // Copy join link
function copyJoinLink() {
  if (!successData?.gameId) return;
  
  const gameCode = successData.gameId;
  const button = event?.currentTarget; 
  
  const originalText = button ? button.textContent : '📋 Copiar';
  
  navigator.clipboard.writeText(gameCode)
    .then(() => {
      if (button) {
        button.textContent = '✅ Copiado!';
        button.style.background = '#10b981';
        button.style.color = 'white';
        
        setTimeout(() => {
          button.textContent = originalText;
          button.style.background = '';
          button.style.color = '';
        }, 5000);
      } else {
        alert('✅ Código copiado: ' + gameCode);
      }
    })
    .catch(() => {
      // FALLBACK para HTTP/localhost
      const textArea = document.createElement('textarea');
      textArea.value = gameCode;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      
      // Feedback visual incluso en fallback
      if (button) {
        button.textContent = '✅ (Copiado manual)';
        setTimeout(() => button.textContent = originalText, 2000);
      }
    });
}

  // Reset for new game
  function createAnother() {
    successData = null;
    gameName = '';
    maxPrice = 20;
    initDate();
  }
</script>

<div class="create-game-form">
  <!-- SUCCESS STATE -->
  {#if successData}
    <div class="success-card">
      <div class="success-icon">🎉</div>
      <h2>¡Juego Creado!</h2>

      <div class="game-info">
        <div class="info-row">
          <span class="label">Código del Juego:</span>
          <span class="value game-code">{successData.gameId}</span>
        </div>
        <div class="info-row">
          <span class="label">Nombre:</span>
          <span class="value">{gameName}</span>
        </div>
        <div class="info-row">
          <span class="label">Precio Máximo:</span>
          <span class="value">{maxPrice}€</span>
        </div>
        <div class="info-row">
          <span class="label">Fecha del Intercambio:</span>
          <span class="value">{new Date(exchangeDate).toLocaleDateString('es-ES')}</span>
        </div>
      </div>

      <div class="share-section">
        <p class="share-title">Comparte este enlace con los participantes:</p>
        <div class="link-box">
          <code>{window.location.origin}/?game={successData.gameId}</code>
          <button on:click|preventDefault={copyJoinLink} class="copy-btn" title="Copiar código">
            📋 Copiar
          </button>
        </div>
      </div>

      <div class="action-buttons">
        <button on:click={createAnother} class="secondary-btn">
          ➕ Crear Otro Juego
        </button>
      </div>
    </div>

  <!-- CREATE FORM -->
  {:else}
    <h2>🎅 Crear un Amigo Invisible</h2>

    {#if error}
      <div class="error-message">
        ⚠️ {error}
      </div>
    {/if}

    <form on:submit|preventDefault={createGame}>
      <div class="form-group">
        <label for="gameName">
          <span class="icon">📝</span> Nombre del Juego
        </label>
        <input
          id="gameName"
          type="text"
          bind:value={gameName}
          placeholder="Ej: Navidad Familiar 2024"
          required
          maxlength="50"
          disabled={loading}
        />
      </div>

      <div class="form-group">
        <label for="maxPrice">
          <span class="icon">💰</span> Precio Máximo: <strong>{maxPrice}€</strong>
        </label>
        <input
          id="maxPrice"
          type="range"
          bind:value={maxPrice}
          min="5"
          max="20"
          step="5"
          disabled={loading}
        />
        <div class="slider-labels">
          <span>5€</span>
          <span>10€</span>
          <span>20€</span>
        </div>
      </div>

      <div class="form-group">
        <label for="exchangeDate">
          <span class="icon">📅</span> Fecha del Intercambio
        </label>
        <input
          id="exchangeDate"
          type="date"
          bind:value={exchangeDate}
          required
          min={new Date().toISOString().split('T')[0]}
          disabled={loading}
        />
      </div>

      <button type="submit" disabled={loading || !gameName.trim()} class="submit-btn">
        {#if loading}
          <span class="spinner"></span> Creando...
        {:else}
          🎄 Crear Juego de Amigo Invisible
        {/if}
      </button>
    </form>
  {/if}
</div>

<style>
  .create-game-form {
    background: rgba(255, 255, 255, 0.98);
    border-radius: 16px;
    padding: 2rem;
    color: #1a472a;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    border: 2px solid #c53030;
    max-width: 500px;
    margin: 0 auto;
  }

  h2 {
    color: #c53030;
    text-align: center;
    margin-bottom: 1.5rem;
    font-size: 1.6rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }

  label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.5rem;
    font-weight: 600;
    color: #2d3748;
  }

  input[type="text"],
  input[type="date"] {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e0;
    border-radius: 8px;
    font-size: 1rem;
    background: white;
    box-sizing: border-box;
  }

  input[type="text"]:focus,
  input[type="date"]:focus {
    outline: none;
    border-color: #c53030;
    box-shadow: 0 0 0 2px rgba(197, 48, 48, 0.1);
  }

  input[type="range"] {
    width: 100%;
    height: 8px;
    background: #e2e8f0;
    border-radius: 4px;
    outline: none;
    -webkit-appearance: none;
  }

  input[type="range"]::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 22px;
    height: 22px;
    background: #c53030;
    border-radius: 50%;
    cursor: pointer;
    border: 3px solid white;
    box-shadow: 0 1px 3px rgba(0,0,0,0.2);
  }

  .slider-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 0.5rem;
    font-size: 0.85rem;
    color: #718096;
  }

  .submit-btn {
    width: 100%;
    padding: 1rem;
    background: #c53030;
    color: white;
    border: none;
    border-radius: 8px;
    font-size: 1.1rem;
    font-weight: bold;
    cursor: pointer;
    margin-top: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
  }

  .submit-btn:hover:not(:disabled) {
    background: #9b2c2c;
  }

  .submit-btn:disabled {
    background: #cbd5e0;
    cursor: not-allowed;
  }

  .spinner {
    width: 18px;
    height: 18px;
    border: 2px solid rgba(255,255,255,0.3);
    border-radius: 50%;
    border-top-color: white;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
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

  .success-card {
    text-align: center;
  }

  .success-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
  }

  .game-info {
    background: #f7fafc;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 1.25rem;
    margin: 1.5rem 0;
    text-align: left;
  }

  .info-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 0.75rem;
    padding-bottom: 0.75rem;
    border-bottom: 1px dashed #e2e8f0;
  }

  .info-row:last-child {
    margin-bottom: 0;
    border-bottom: none;
  }

  .label {
    font-weight: 600;
    color: #4a5568;
  }

  .value {
    color: #2d3748;
  }

  .game-code {
    font-family: 'Courier New', monospace;
    background: #edf2f7;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    font-weight: bold;
  }

  .share-section {
    margin: 1.5rem 0;
  }

  .share-title {
    font-weight: 600;
    margin-bottom: 0.75rem;
    color: #4a5568;
  }

  .link-box {
    display: flex;
    gap: 0.5rem;
    align-items: center;
    background: #edf2f7;
    padding: 0.75rem;
    border-radius: 8px;
  }

  .link-box code {
    flex: 1;
    font-family: monospace;
    font-size: 0.85rem;
    color: #2d3748;
    word-break: break-all;
  }

  .copy-btn {
    background: #4299e1;
    color: white;
    border: none;
    padding: 0.5rem 0.75rem;
    border-radius: 6px;
    cursor: pointer;
    white-space: nowrap;
  }

  .copy-btn:hover {
    background: #3182ce;
  }

  .action-buttons {
    margin-top: 1.5rem;
  }

  .secondary-btn {
    width: 100%;
    padding: 0.75rem;
    background: #e2e8f0;
    color: #4a5568;
    border: 1px solid #cbd5e0;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
  }

  .secondary-btn:hover {
    background: #cbd5e0;
  }
</style>