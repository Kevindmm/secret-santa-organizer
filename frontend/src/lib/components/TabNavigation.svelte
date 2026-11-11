<!-- frontend/src/lib/components/TabNavigation.svelte -->
<script>
  import { createEventDispatcher } from 'svelte';
  import { Gift, Link2 } from 'lucide-svelte';

  // Export current active tab
  export let activeTab = 'create';

  const dispatch = createEventDispatcher();

  const tabs = [
    { id: 'create', label: 'Crear intercambio', icon: Gift },
    { id: 'join', label: 'Unirme', icon: Link2 }
  ];

  /** @param {string} tabId */
  function selectTab(tabId) {
    activeTab = tabId;
    dispatch('tabchange', { tab: tabId });
  }
</script>

<div class="tab-navigation" role="tablist" aria-label="Acciones del organizador">
  {#each tabs as tab (tab.id)}
    <button
      type="button"
      role="tab"
      class:active={activeTab === tab.id}
      aria-selected={activeTab === tab.id}
      aria-controls="tab-content"
      on:click={() => selectTab(tab.id)}
    >
      <svelte:component this={tab.icon} size={18} strokeWidth={1.9} aria-hidden="true" />
      <span class="label">{tab.label}</span>
    </button>
  {/each}
</div>

<style>
  .tab-navigation {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 4px;
    padding: 4px;
    margin-bottom: 1.25rem;
    background: rgba(255, 255, 255, 0.045);
    border: 1px solid var(--line);
    border-radius: 20px;
    box-shadow: 0 1px 0 rgba(255, 255, 255, 0.04) inset;
  }

  button {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    min-height: 52px;
    padding: 0.6rem 0.75rem;
    color: var(--muted);
    background: transparent;
    border: 0;
    border-radius: 16px;
    font-size: 0.9rem;
    font-weight: 600;
    line-height: 1.2;
    transition:
      color 0.2s ease,
      background-color 0.2s ease,
      box-shadow 0.2s ease,
      transform 0.18s var(--ease-out);
  }

  button:active {
    transform: scale(0.99);
  }

  button.active {
    color: var(--text);
    background: linear-gradient(160deg, rgba(255, 255, 255, 0.14), rgba(255, 255, 255, 0.06));
    box-shadow:
      0 1px 0 rgba(255, 255, 255, 0.12) inset,
      0 12px 26px -16px rgba(0, 0, 0, 0.9);
  }

  button.active :global(svg) {
    color: var(--gold);
  }

  .label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  @media (hover: hover) {
    button:hover:not(.active) {
      color: var(--text-soft);
      background: rgba(255, 255, 255, 0.04);
    }
  }

  @media (max-width: 380px) {
    button {
      gap: 0.35rem;
      padding-inline: 0.5rem;
      font-size: 0.82rem;
    }
  }
</style>
