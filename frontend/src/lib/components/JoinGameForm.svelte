<!-- frontend/src/lib/components/JoinGameForm.svelte -->
<script>
  // State
  let gameCode = '';
  let participantName = '';
  let participantEmail = '';
  let participantWishlist = '';
  let loading = false;
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
  const response = await fetch(`${import.meta.env.PUBLIC_API_URL}/api/games/${formattedGameCode}/participants`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      name: participantName,
      email: participantEmail,
      wishList: participantWishlist
    })
  });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Error (${response.status}): ${errText}`);
      }

      success = true;

    } catch (err) {
      error = err.message.includes('404')
        ? 'Juego no encontrado. Verifica el código.'
        : err.message.includes('duplicate')
        ? 'Este email ya está registrado en el juego.'
        : 'Error al unirse al juego. Verifica el código.';
    } finally {
      loading = false;
    }
  }

  // Simple email validation
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

<div class="join-game-form">
  <!-- SUCCESS STATE -->
  {#if success}
    <div class="success-card">
      <div class="success-icon">✅</div>
      <h2>¡Te has unido al juego!</h2>

      <div class="success-details">
        <p><strong>Nombre:</strong> {participantName}</p>
        <p><strong>Juego:</strong> Código {formattedGameCode}</p>
      </div>

      <div class="instructions">
        <h3>🎁 ¿Qué sigue?</h3>
        <ul>
          <li>Espera a que el organizador asigne los amigos secretos</li>
          <li>Recibirás un email con la persona que te ha tocado</li>
          <li>¡Mantén el secreto hasta el día del intercambio!</li>
        </ul>
      </div>

      <div class="action-buttons">
        <button on:click={joinAnother} class="secondary-btn">
          🔗 Unirse a Otro Juego
        </button>
      </div>
    </div>

  <!-- JOIN FORM -->
  {:else}
    <h2>🎄 Unirse a un Juego Existente</h2>

    {#if error}
      <div class="error-message">
        ⚠️ {error}
      </div>
    {/if}

    <form on:submit|preventDefault={joinGame}>
      <!-- Game Code -->
      <div class="form-group">
        <label for="gameCode">
          <span class="icon">🔑</span> Código del Juego
        </label>
        <input
          id="gameCode"
          type="text"
          bind:value={gameCode}
          placeholder="Ej: X8B3K9"
          required
          maxlength="10"
          disabled={loading}
        />
        <div class="hint">Pide el código al organizador del juego</div>
      </div>

      <!-- Participant Name -->
      <div class="form-group">
        <label for="participantName">
          <span class="icon">👤</span> Tu Nombre
        </label>
        <input
          id="participantName"
          type="text"
          bind:value={participantName}
          placeholder="Tu nombre completo"
          required
          maxlength="50"
          disabled={loading}
        />
      </div>

      <!-- Participant Email -->
      <div class="form-group">
        <label for="participantEmail">
          <span class="icon">📧</span> Tu Email
        </label>
        <input
          id="participantEmail"
          type="email"
          bind:value={participantEmail}
          placeholder="tucorreo@ejemplo.com"
          required
          disabled={loading}
        />
        <div class="hint">Recibirás aquí la asignación de tu amigo secreto</div>
      </div>

      <!-- Wishlist -->
      <div class="form-group">
        <label for="participantWishlist">
          <span class="icon">🎁</span> Tu Lista de Deseos (Opcional)
        </label>
        <textarea
          id="participantWishlist"
          bind:value={participantWishlist}
          placeholder="¿Qué te gustaría recibir? Ej: libros, chocolate, una bufanda..."
          rows="3"
          maxlength="200"
          disabled={loading}
        ></textarea>
        <div class="hint">Ayuda a tu amigo secreto con algunas ideas</div>
      </div>

      <!-- Submit Button -->
      <button type="submit" disabled={loading || !gameCode.trim() || !participantName.trim() || !participantEmail.trim()}
              class="submit-btn">
        {#if loading}
          <span class="spinner"></span> Uniéndose...
        {:else}
          🎅 Unirse al Juego
        {/if}
      </button>
    </form>
  {/if}
</div>

<style>
  .join-game-form {
    background: rgba(255, 255, 255, 0.98);
    border-radius: 16px;
    padding: 2rem;
    color: #1a472a;
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
    border: 2px solid #2d5c2d;
    max-width: 500px;
    margin: 0 auto;
  }

  h2 {
    color: #2d5c2d;
    text-align: center;
    margin-bottom: 1.5rem;
    font-size: 1.6rem;
  }

  /* Form Groups */
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
  input[type="email"],
  textarea {
    width: 100%;
    padding: 0.75rem;
    border: 1px solid #cbd5e0;
    border-radius: 8px;
    font-size: 1rem;
    background: white;
    box-sizing: border-box;
    font-family: inherit;
  }

  input[type="text"]:focus,
  input[type="email"]:focus,
  textarea:focus {
    outline: none;
    border-color: #2d5c2d;
    box-shadow: 0 0 0 2px rgba(45, 92, 45, 0.1);
  }

  textarea {
    resize: vertical;
    min-height: 80px;
  }

  /* Hint text */
  .hint {
    font-size: 0.85rem;
    color: #718096;
    margin-top: 0.3rem;
    font-style: italic;
  }

  /* Submit Button */
  .submit-btn {
    width: 100%;
    padding: 1rem;
    background: #2d5c2d;
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
    background: #1e3e1e;
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

  /* Error Message */
  .error-message {
    background: #fed7d7;
    border: 1px solid #fc8181;
    color: #9b2c2c;
    padding: 0.75rem;
    border-radius: 8px;
    margin-bottom: 1rem;
    text-align: center;
  }

  /* Success Card */
  .success-card {
    text-align: center;
  }

  .success-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
  }

  .success-details {
    background: #f0fff4;
    border: 1px solid #9ae6b4;
    border-radius: 12px;
    padding: 1.25rem;
    margin: 1.5rem 0;
    text-align: left;
  }

  .success-details p {
    margin: 0.5rem 0;
  }

  .instructions {
    background: #f7fafc;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 1.25rem;
    margin: 1.5rem 0;
    text-align: left;
  }

  .instructions h3 {
    color: #2d5c2d;
    margin-top: 0;
    margin-bottom: 0.75rem;
  }

  .instructions ul {
    margin: 0;
    padding-left: 1.25rem;
    color: #4a5568;
  }

  .instructions li {
    margin-bottom: 0.5rem;
  }

  .instructions li:last-child {
    margin-bottom: 0;
  }

  /* Action Buttons */
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