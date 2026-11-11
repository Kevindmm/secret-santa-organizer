<script>
  import { onMount } from 'svelte';
  import { API_URL } from '$lib/config';

  const TIMEOUT_MS = 6000;
  const RETRY_MS = 4000;
  const SLOW_AFTER_MS = 15000;
  const HIDE_AFTER_MS = 4000;

  let status = $state('down');
  let visible = $state(true);
  let slow = $state(false);

  /** @type {ReturnType<typeof setTimeout> | undefined} */
  let retryTimer;
  /** @type {ReturnType<typeof setTimeout> | undefined} */
  let hideTimer;
  /** @type {ReturnType<typeof setTimeout> | undefined} */
  let slowTimer;
  /** @type {AbortController | undefined} */
  let controller;

  async function check() {
    controller?.abort();
    controller = new AbortController();
    const requestTimeout = setTimeout(() => controller?.abort(), TIMEOUT_MS);

    try {
      const response = await fetch(`${API_URL}/actuator/health/liveness`, {
        signal: controller.signal,
        headers: { accept: 'application/json' }
      });
      status = response.ok ? 'up' : 'down';
    } catch {
      status = 'down';
    } finally {
      clearTimeout(requestTimeout);
    }

    if (status === 'up') {
      clearTimeout(slowTimer);
      slow = false;
      hideTimer = setTimeout(() => {
        visible = false;
      }, HIDE_AFTER_MS);
      return;
    }

    retryTimer = setTimeout(check, RETRY_MS);
  }

  onMount(() => {
    check();
    slowTimer = setTimeout(() => {
      slow = true;
    }, SLOW_AFTER_MS);

    return () => {
      controller?.abort();
      clearTimeout(retryTimer);
      clearTimeout(hideTimer);
      clearTimeout(slowTimer);
    };
  });
</script>

{#if visible}
  <div
    class="status"
    class:is-up={status === 'up'}
    role="status"
    aria-live="polite"
  >
    <span class="dot" aria-hidden="true"></span>
    <span class="text">
      {#if status === 'up'}
        Servidor conectado
      {:else if slow}
        El servidor está despertando, puede tardar un minuto
      {:else}
        Servidor iniciándose, espera un momento
      {/if}
    </span>
  </div>
{/if}

<style>
  .status {
    position: fixed;
    left: 14px;
    bottom: calc(14px + env(safe-area-inset-bottom));
    z-index: 60;
    display: flex;
    align-items: center;
    gap: 8px;
    max-width: min(300px, calc(100vw - 28px));
    padding: 8px 12px;
    border: 1px solid var(--line);
    border-radius: 999px;
    background: rgba(7, 19, 13, 0.9);
    box-shadow: var(--shadow-sm);
    color: var(--text-soft);
    font-size: 12px;
    line-height: 1.35;
    pointer-events: none;
    backdrop-filter: blur(8px);
  }

  .dot {
    flex: 0 0 auto;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--danger);
    box-shadow: 0 0 0 3px rgba(240, 122, 116, 0.16);
    animation: pulse 1.8s ease-in-out infinite;
  }

  .is-up .dot {
    background: var(--success);
    box-shadow: 0 0 0 3px rgba(102, 199, 154, 0.16);
    animation: none;
  }

  .text {
    min-width: 0;
  }

  @keyframes pulse {
    0%,
    100% {
      opacity: 1;
    }
    50% {
      opacity: 0.45;
    }
  }

  @media (prefers-reduced-motion: reduce) {
    .dot {
      animation: none;
    }
  }
</style>
