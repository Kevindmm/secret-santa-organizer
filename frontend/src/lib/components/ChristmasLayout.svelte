<!-- frontend/src/lib/components/ChristmasLayout.svelte -->
<script>
  import { onMount } from 'svelte';
  import SantaPanel from './SantaPanel.svelte'; // Importar el componente

  export let title = "🎄 Organizador de Amigo Invisible";

  // Generate snowflakes
  let snowflakes = [];

  onMount(() => {
    // Create snowflakes
    snowflakes = Array.from({ length: 80 }, (_, i) => ({
      id: i,
      left: Math.random() * 100,
      size: 12 + Math.random() * 20,
      speed: 3 + Math.random() * 4,
      delay: Math.random() * 15,
      opacity: 0.2 + Math.random() * 0.5,
      sway: 1 + Math.random() * 2,
      type: getRandomSnowflakeType()
    }));

    // Create stars
    createStars();
  });

  // Snowflake types
  function getRandomSnowflakeType() {
    const types = ['❄', '❅', '❆', '＊', '✱', '✦'];
    return types[Math.floor(Math.random() * types.length)];
  }

  // Create stars
  function createStars() {
    const starsContainer = document.querySelector('.stars');
    if (!starsContainer) return;

    for (let i = 0; i < 25; i++) {
      const star = document.createElement('div');
      star.className = 'star';
      star.style.left = `${Math.random() * 100}%`;
      star.style.top = `${Math.random() * 100}%`;
      star.style.animationDelay = `${Math.random() * 5}s`;
      star.style.width = star.style.height = `${1 + Math.random() * 3}px`;
      starsContainer.appendChild(star);
    }
  }

  // Handle assignment completion
  function handleAssignment(event) {
    console.log('Amigos secretos asignados para:', event.detail.gameCode);
  }
</script>

<div class="christmas-layout">
  <!-- Stars -->
  <div class="stars"></div>

  <!-- Snowflakes -->
  <div class="snowflakes">
    {#each snowflakes as flake}
      <div
        class="snowflake"
        style="
          left: {flake.left}%;
          font-size: {flake.size}px;
          animation-duration: {15 + flake.speed * 8}s;
          animation-delay: {flake.delay}s;
          opacity: {flake.opacity};
          --sway: {flake.sway};
        "
      >{flake.type}</div>
    {/each}
  </div>

  <!-- Decorations -->
  <div class="decoration decoration-1">🎁</div>
  <div class="decoration decoration-2">✨</div>
  <div class="decoration decoration-3">🌟</div>
  <div class="decoration decoration-4">🕯️</div>

  <!-- Include SantaPanel component -->
  <SantaPanel on:assigned={handleAssignment} />

  <!-- Content -->
  <div class="content-wrapper">
    <header>
      <h1>{title}</h1>
      <p class="subtitle">Crea o únete a un intercambio de regalos navideño 🎁</p>
    </header>

    <main>
      <slot />
    </main>

    <footer>
      <p>✨ Que la magia de la Navidad esté con vosotros ✨</p>
    </footer>
  </div>
</div>

<style>
  .christmas-layout {
    background:
      radial-gradient(ellipse at 20% 20%, rgba(255, 215, 0, 0.08) 0%, transparent 40%),
      radial-gradient(ellipse at 80% 80%, rgba(255, 0, 0, 0.08) 0%, transparent 40%),
      linear-gradient(135deg, #0a2e0a 0%, #1a472a 40%, #0a2e0a 100%);
    color: #fff;
    height: 100vh;
    width: 100vw;
    position: fixed;
    top: 0;
    left: 0;
    overflow: hidden;
    margin: 0;
    padding: 0;
  }

  /* Santa Panel Button (now handled inside SantaPanel.svelte) */

  /* Stars */
  .stars {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;
  }

  .star {
    position: absolute;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 50%;
    animation: twinkle 4s infinite alternate ease-in-out;
    opacity: 0;
    box-shadow: 0 0 4px rgba(255, 255, 255, 0.5);
  }

  @keyframes twinkle {
    0%, 100% {
      opacity: 0;
      transform: scale(0.7);
    }
    50% {
      opacity: 0.7;
      transform: scale(1.2);
    }
  }

  /* Snowflakes */
  .snowflakes {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 1;
  }

  .snowflake {
    position: absolute;
    top: -50px;
    color: rgba(255, 255, 255, 0.9);
    text-shadow:
      0 0 8px rgba(255, 255, 255, 0.7),
      0 0 16px rgba(173, 216, 230, 0.5);
    animation: snowfall linear infinite;
    user-select: none;
    filter: blur(0.3px);
  }

  @keyframes snowfall {
    0% {
      transform: translateY(-50px) translateX(0) rotate(0deg);
      opacity: 0;
    }
    10% {
      opacity: var(--flake-opacity, 0.7);
    }
    90% {
      opacity: var(--flake-opacity, 0.7);
    }
    100% {
      transform:
        translateY(calc(100vh + 50px))
        translateX(calc(var(--sway, 1) * 100px))
        rotate(720deg);
      opacity: 0;
    }
  }

  /* Decorations */
  .decoration {
    position: absolute;
    font-size: 1.8rem;
    opacity: 0.1;
    filter: blur(0.5px);
    z-index: 0;
    animation: decorationFloat 25s ease-in-out infinite;
  }

  .decoration-1 { top: 15%; left: 8%; animation-delay: 0s; color: #ff4444; }
  .decoration-2 { top: 70%; right: 12%; animation-delay: 7s; color: #ffd700; }
  .decoration-3 { bottom: 20%; left: 15%; animation-delay: 14s; color: #ffffff; }
  .decoration-4 { top: 35%; right: 5%; animation-delay: 21s; color: #44aaff; }

  @keyframes decorationFloat {
    0%, 100% {
      transform: translateY(0) rotate(0deg) scale(1);
      opacity: 0.1;
    }
    25% {
      transform: translateY(-30px) rotate(90deg) scale(1.1);
      opacity: 0.15;
    }
    50% {
      transform: translateY(0) rotate(180deg) scale(1);
      opacity: 0.1;
    }
    75% {
      transform: translateY(30px) rotate(270deg) scale(0.9);
      opacity: 0.15;
    }
  }

  /* Content wrapper */
  .content-wrapper {
    position: relative;
    z-index: 2;
    height: 100vh;
    display: flex;
    flex-direction: column;
    padding: 1rem;
    box-sizing: border-box;
    overflow-y: auto;
  }

  /* Header */
  header {
    text-align: center;
    margin-bottom: 1.5rem;
    flex-shrink: 0;
  }

  h1 {
    font-size: clamp(1.8rem, 4vw, 2.5rem);
    color: #ffd700;
    margin: 0.5rem 0;
    text-shadow:
      1px 1px 3px rgba(0, 0, 0, 0.3),
      0 0 10px rgba(255, 215, 0, 0.3);
    line-height: 1.2;
  }

  .subtitle {
    font-size: clamp(0.9rem, 2vw, 1.1rem);
    color: #c6f6d5;
    opacity: 0.9;
    margin: 0;
    text-shadow: 0 0 5px rgba(198, 246, 213, 0.3);
  }

  /* Main content */
  main {
    flex: 1;
    max-width: 600px;
    width: 100%;
    margin: 0 auto;
    display: flex;
    flex-direction: column;
  }

  main > * {
    max-height: 100%;
    overflow-y: auto;
  }

  /* Footer */
  footer {
    text-align: center;
    padding: 1rem 0;
    margin-top: auto;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    color: #c6f6d5;
    font-size: clamp(0.8rem, 2vw, 0.95rem);
    flex-shrink: 0;
    text-shadow: 0 0 5px rgba(198, 246, 213, 0.2);
  }

  /* Responsive */
  @media (max-width: 768px) {
    .decoration {
      font-size: 1.4rem;
    }

    .snowflake {
      font-size: 14px !important;
    }
  }

  @media (max-height: 700px) {
    header {
      margin-bottom: 0.5rem;
    }

    h1 {
      font-size: 1.5rem;
    }

    .snowflakes {
      opacity: 0.7;
    }
  }
</style>