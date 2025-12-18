<!-- frontend/src/routes/+page.svelte -->
<script>
  import ChristmasLayout from '$lib/components/ChristmasLayout.svelte';
  import TabNavigation from '$lib/components/TabNavigation.svelte';
  import CreateGameForm from '$lib/components/CreateGameForm.svelte';
  import JoinGameForm from '$lib/components/JoinGameForm.svelte';

  let activeTab = 'create';

  function handleTabChange(event) {
    activeTab = event.detail.tab;
  }
</script>

<ChristmasLayout>
  <TabNavigation
    bind:activeTab
    on:tabchange={handleTabChange}
  />

  <div class="content-container">
    {#if activeTab === 'create'}
      <CreateGameForm />
    {:else}
      <JoinGameForm />
    {/if}
  </div>
</ChristmasLayout>

<style>
  .page-container {
    min-height: 100vh;
    background: linear-gradient(135deg, #0f2f0f 0%, #1a472a 100%);
    color: white;
    padding: 2rem 1rem;
    display: flex;
    flex-direction: column;
  }

  .header {
    text-align: center;
    margin-bottom: 2rem;
  }

  h1 {
    color: #ffd700;
    font-size: 2.2rem;
    margin-bottom: 0.5rem;
    text-shadow: 1px 1px 3px rgba(0, 0, 0, 0.3);
  }

  .subtitle {
    color: #c6f6d5;
    font-size: 1.1rem;
    opacity: 0.9;
  }

  .main-content {
    flex: 1;
    max-width: 600px;
    width: 100%;
    margin: 0 auto;
  }

  .tab-content {
    animation: fadeIn 0.3s ease;
  }

  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
  }

  .footer {
    text-align: center;
    margin-top: 3rem;
    padding-top: 1.5rem;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    color: #c6f6d5;
    font-style: italic;
    font-size: 0.9rem;
  }

  /* Responsive design */
  @media (max-width: 640px) {
    .page-container {
      padding: 1.5rem 1rem;
    }

    h1 {
      font-size: 1.8rem;
    }

    .subtitle {
      font-size: 1rem;
    }
  }

  @media (max-width: 480px) {
    h1 {
      font-size: 1.5rem;
    }

    .page-container {
      padding: 1rem 0.75rem;
    }
  }
</style>