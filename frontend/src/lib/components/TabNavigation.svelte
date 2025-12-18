<!-- frontend/src/lib/components/TabNavigation.svelte -->
<script>
  // Export current active tab
  export let activeTab = 'create';

  // Tab configuration
  const tabs = [
    { id: 'create', label: 'Crear Juego', icon: '🎁' },
    { id: 'join', label: 'Unirse a Juego', icon: '🔗' }
  ];

  // Emit tab change event
  import { createEventDispatcher } from 'svelte';
  const dispatch = createEventDispatcher();

  function selectTab(tabId) {
    activeTab = tabId;
    dispatch('tabchange', { tab: tabId });
  }
</script>

<div class="tab-navigation">
  {#each tabs as tab (tab.id)}
    <button
      class:active={activeTab === tab.id}
      on:click={() => selectTab(tab.id)}
      aria-selected={activeTab === tab.id}
      aria-controls="tab-content"
    >
      <span class="icon">{tab.icon}</span>
      <span class="label">{tab.label}</span>
      {#if activeTab === tab.id}
        <span class="indicator"></span>
      {/if}
    </button>
  {/each}
</div>

<style>
  .tab-navigation {
    display: flex;
    gap: 0.5rem;
    background: #f7fafc;
    padding: 0.5rem;
    border-radius: 12px;
    border: 1px solid #e2e8f0;
    margin-bottom: 2rem;
    max-width: 400px;
    margin-left: auto;
    margin-right: auto;
  }

  button {
    flex: 1;
    position: relative;
    padding: 0.875rem 1rem;
    background: transparent;
    border: none;
    border-radius: 8px;
    color: #718096;
    font-size: 1rem;
    font-weight: 600;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.5rem;
    transition: all 0.2s ease;
  }

  button:hover:not(.active) {
    background: #edf2f7;
    color: #4a5568;
  }

  button.active {
    background: white;
    color: #2d3748;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  button.active .icon {
    color: #c53030;
  }

  button.active .label {
    color: #2d3748;
  }

  .icon {
    font-size: 1.25rem;
    transition: color 0.2s ease;
  }

  .label {
    transition: color 0.2s ease;
  }

  .indicator {
    position: absolute;
    bottom: -0.5rem;
    left: 50%;
    transform: translateX(-50%);
    width: 4px;
    height: 4px;
    background: #c53030;
    border-radius: 50%;
  }

  /* Focus styles for accessibility */
  button:focus {
    outline: 2px solid #4299e1;
    outline-offset: 2px;
  }

  /* Mobile responsiveness */
  @media (max-width: 480px) {
    .tab-navigation {
      padding: 0.375rem;
      gap: 0.25rem;
    }

    button {
      padding: 0.75rem 0.5rem;
      font-size: 0.9rem;
    }

    .icon {
      font-size: 1.1rem;
    }
  }
</style>