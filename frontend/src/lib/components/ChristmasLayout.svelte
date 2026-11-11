<!-- frontend/src/lib/components/ChristmasLayout.svelte -->
<script>
  import { onMount } from 'svelte';
  import { Snowflake } from 'lucide-svelte';
  import SantaPanel from './SantaPanel.svelte';

  export let title = 'Amigo Invisible';

  /**
   * @typedef {{ id: number, left: number, size: number, duration: number, delay: number, opacity: number, drift: number, type: string }} Snowflake
   */

  /** @type {Snowflake[]} */
  let snowflakes = [];

  onMount(() => {
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduceMotion) return;

    // Nieve escasa y discreta: suficiente para dar ambiente, sin tapar el contenido.
    snowflakes = Array.from({ length: 22 }, (_, i) => ({
      id: i,
      left: Math.random() * 100,
      size: 7 + Math.random() * 10,
      duration: 18 + Math.random() * 18,
      delay: Math.random() * 18,
      opacity: 0.1 + Math.random() * 0.28,
      drift: -50 + Math.random() * 100,
      type: getRandomSnowflakeType()
    }));
  });

  function getRandomSnowflakeType() {
    const types = ['❄', '❅', '❆', '✦'];
    return types[Math.floor(Math.random() * types.length)];
  }

  /** @param {CustomEvent<{ gameCode: string }>} event */
  function handleAssignment(event) {
    console.log('Amigos secretos asignados para:', event.detail.gameCode);
  }
</script>

<div class="christmas-layout">
  <div class="ambient" aria-hidden="true">
    <span class="glow glow-gold"></span>
    <span class="glow glow-red"></span>

    <div class="snow">
      {#each snowflakes as flake (flake.id)}
        <span
          class="snowflake"
          style="
            left: {flake.left}%;
            font-size: {flake.size}px;
            opacity: {flake.opacity};
            animation-duration: {flake.duration}s;
            animation-delay: -{flake.delay}s;
            --drift: {flake.drift}px;
          "
        >{flake.type}</span>
      {/each}
    </div>
  </div>

  <div class="app-shell">
    <header class="app-header">
      <div class="app-header-inner">
        <div class="brand">
          <span class="brand-mark" aria-hidden="true">
            <Snowflake size={20} strokeWidth={1.75} />
          </span>
          <span class="brand-copy">
            <span class="brand-eyebrow">Secret Santa</span>
            <span class="brand-title">{title}</span>
          </span>
        </div>

        <SantaPanel on:assigned={handleAssignment} />
      </div>
    </header>

    <main class="app-main">
      <slot />
    </main>

    <footer class="app-footer">
      <p>Hecho para que solo te preocupes de regalar.</p>
    </footer>
  </div>
</div>

<style>
  .christmas-layout {
    position: relative;
    isolation: isolate;
    min-height: 100vh;
    min-height: 100dvh;
    background:
      radial-gradient(ellipse 80% 55% at 50% -10%, rgba(45, 122, 85, 0.28), transparent 70%),
      linear-gradient(180deg, #081810 0%, #07130d 45%, #061009 100%);
  }

  .ambient {
    position: fixed;
    inset: 0;
    z-index: 0;
    overflow: hidden;
    pointer-events: none;
  }

  .glow {
    position: absolute;
    width: 70vw;
    height: 70vw;
    max-width: 720px;
    max-height: 720px;
    border-radius: 50%;
    filter: blur(90px);
  }

  .glow-gold {
    top: -24%;
    right: -28%;
    background: #c79b3a;
    opacity: 0.15;
  }

  .glow-red {
    bottom: -30%;
    left: -32%;
    background: #a3312d;
    opacity: 0.11;
  }

  .snow {
    position: absolute;
    inset: 0;
  }

  .snowflake {
    position: absolute;
    top: -8vh;
    color: rgba(255, 255, 255, 0.9);
    animation-name: snowfall;
    animation-timing-function: linear;
    animation-iteration-count: infinite;
    user-select: none;
  }

  .app-shell {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    min-height: 100dvh;
  }

  .app-header {
    position: sticky;
    top: 0;
    z-index: 50;
    padding-top: calc(0.7rem + var(--safe-top));
    padding-right: max(1rem, env(safe-area-inset-right));
    padding-bottom: 0.7rem;
    padding-left: max(1rem, env(safe-area-inset-left));
    background: linear-gradient(180deg, rgba(7, 19, 13, 0.94) 0%, rgba(7, 19, 13, 0.68) 100%);
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    backdrop-filter: blur(18px) saturate(120%);
    -webkit-backdrop-filter: blur(18px) saturate(120%);
  }

  .app-header-inner {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 0.75rem;
    width: 100%;
    max-width: 720px;
    margin: 0 auto;
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 0.7rem;
    min-width: 0;
  }

  .brand-mark {
    display: grid;
    flex: none;
    place-items: center;
    width: 42px;
    height: 42px;
    color: var(--gold);
    background: linear-gradient(145deg, rgba(233, 203, 123, 0.18), rgba(233, 203, 123, 0.05));
    border: 1px solid rgba(233, 203, 123, 0.24);
    border-radius: 14px;
    box-shadow: 0 1px 0 rgba(255, 255, 255, 0.12) inset;
  }

  .brand-copy {
    display: flex;
    flex-direction: column;
    min-width: 0;
    line-height: 1.15;
  }

  .brand-eyebrow {
    color: var(--gold);
    font-size: 0.62rem;
    font-weight: 700;
    letter-spacing: 0.16em;
    text-transform: uppercase;
  }

  .brand-title {
    overflow: hidden;
    color: var(--text);
    font-size: 0.98rem;
    font-weight: 650;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .app-main {
    flex: 1;
    width: 100%;
    max-width: 720px;
    margin: 0 auto;
    padding: 1.5rem max(1rem, env(safe-area-inset-left)) 2.5rem max(1rem, env(safe-area-inset-right));
  }

  .app-footer {
    width: 100%;
    max-width: 720px;
    margin: 0 auto;
    padding: 1rem 1.25rem calc(1.25rem + var(--safe-bottom));
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    color: var(--muted);
    font-size: 0.76rem;
    text-align: center;
  }

  .app-footer p {
    margin: 0;
  }

  @keyframes snowfall {
    from {
      transform: translate3d(0, -10vh, 0) rotate(0deg);
    }
    to {
      transform: translate3d(var(--drift, 40px), 112vh, 0) rotate(320deg);
    }
  }

  @media (min-width: 640px) {
    .app-main {
      padding-top: 2.25rem;
      padding-right: 1.5rem;
      padding-left: 1.5rem;
    }

    .brand-title {
      font-size: 1.05rem;
    }
  }

  @media (max-width: 380px) {
    .brand-mark {
      width: 38px;
      height: 38px;
    }

    .brand-title {
      font-size: 0.9rem;
    }
  }
</style>
