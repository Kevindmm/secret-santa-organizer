<!-- frontend/src/routes/+page.svelte -->
<script>
  import { Mail, Shuffle, Sparkles, Users } from 'lucide-svelte';
  import ChristmasLayout from '$lib/components/ChristmasLayout.svelte';
  import TabNavigation from '$lib/components/TabNavigation.svelte';
  import CreateGameForm from '$lib/components/CreateGameForm.svelte';
  import JoinGameForm from '$lib/components/JoinGameForm.svelte';

  let activeTab = 'create';

  /** @param {CustomEvent<{ tab: string }>} event */
  function handleTabChange(event) {
    activeTab = event.detail.tab;
  }
</script>

<ChristmasLayout>
  <section class="hero">
    <p class="eyebrow">
      <Sparkles size={14} strokeWidth={2} aria-hidden="true" />
      Navidad sin estrés
    </p>

    <h1>
      Un amigo invisible
      <span class="accent">que empieza en un minuto.</span>
    </h1>

    <p class="hero-lede">
      Crea el intercambio, comparte el código y deja que el sorteo y los emails
      se hagan solos. Sin hojas de cálculo, sin líos.
    </p>

    <ul class="hero-facts" aria-label="Ventajas">
      <li><Users size={15} aria-hidden="true" /> Desde 3 personas</li>
      <li><Shuffle size={15} aria-hidden="true" /> Sorteo justo</li>
      <li><Mail size={15} aria-hidden="true" /> Emails automáticos</li>
    </ul>
  </section>

  <section class="workspace" aria-label="Organiza tu intercambio">
    <TabNavigation bind:activeTab on:tabchange={handleTabChange} />

    <div id="tab-content" class="content-container fade-up" role="tabpanel">
      {#if activeTab === 'create'}
        <CreateGameForm />
      {:else}
        <JoinGameForm />
      {/if}
    </div>
  </section>

  <section class="how" aria-labelledby="how-title">
    <div class="how-head">
      <p class="eyebrow">Cómo funciona</p>
      <h2 id="how-title">Tres pasos, cero fricción.</h2>
    </div>

    <ol class="steps">
      <li>
        <span class="step-index" aria-hidden="true">01</span>
        <div>
          <h3>Crea y comparte</h3>
          <p>Define presupuesto y fecha. Recibirás un código para enviar al grupo.</p>
        </div>
      </li>
      <li>
        <span class="step-index" aria-hidden="true">02</span>
        <div>
          <h3>Todos se apuntan</h3>
          <p>Cada persona deja su nombre, email y una lista de deseos opcional.</p>
        </div>
      </li>
      <li>
        <span class="step-index" aria-hidden="true">03</span>
        <div>
          <h3>Santa hace el resto</h3>
          <p>El sorteo es aleatorio y cada participante recibe su asignación por email.</p>
        </div>
      </li>
    </ol>
  </section>
</ChristmasLayout>

<style>
  .hero {
    padding: 0.25rem 0 1.75rem;
    animation: fade-up 0.45s var(--ease-out) both;
  }

  .hero h1 {
    margin: 0.65rem 0 0.85rem;
    font-size: clamp(2rem, 8.5vw, 3.1rem);
    font-weight: 750;
    line-height: 1.04;
    letter-spacing: -0.04em;
    text-wrap: balance;
  }

  .hero h1 .accent {
    display: block;
    color: var(--gold);
    background: linear-gradient(105deg, #f7e9bb 0%, #e9cb7b 42%, #d8a844 100%);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .hero-lede {
    max-width: 54ch;
    margin: 0 0 1.25rem;
    color: var(--text-soft);
    font-size: 0.98rem;
    line-height: 1.6;
  }

  .hero-facts {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    padding: 0;
    margin: 0;
    list-style: none;
  }

  .hero-facts li {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    padding: 0.42rem 0.7rem;
    color: var(--text-soft);
    background: rgba(255, 255, 255, 0.045);
    border: 1px solid rgba(255, 255, 255, 0.075);
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .hero-facts :global(svg) {
    color: var(--gold);
  }

  .workspace {
    margin-top: 0.25rem;
  }

  .content-container {
    min-width: 0;
  }

  .how {
    margin-top: 2.75rem;
    padding-top: 2rem;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
  }

  .how-head {
    margin-bottom: 1rem;
  }

  .how-head h2 {
    margin: 0.4rem 0 0;
    font-size: clamp(1.3rem, 5vw, 1.65rem);
    font-weight: 700;
    letter-spacing: -0.025em;
  }

  .steps {
    display: grid;
    gap: 0.65rem;
    padding: 0;
    margin: 0;
    list-style: none;
  }

  .steps li {
    display: flex;
    align-items: flex-start;
    gap: 0.85rem;
    padding: 0.95rem 1rem;
    background: linear-gradient(150deg, rgba(255, 255, 255, 0.055), rgba(255, 255, 255, 0.022));
    border: 1px solid rgba(255, 255, 255, 0.075);
    border-radius: 18px;
  }

  .step-index {
    display: grid;
    flex: none;
    place-items: center;
    width: 32px;
    height: 32px;
    color: var(--gold);
    background: var(--gold-soft);
    border: 1px solid rgba(233, 203, 123, 0.2);
    border-radius: 10px;
    font-size: 0.68rem;
    font-weight: 700;
    letter-spacing: 0.04em;
  }

  .steps h3 {
    margin: 0 0 0.15rem;
    font-size: 0.94rem;
    font-weight: 650;
    letter-spacing: -0.01em;
  }

  .steps p {
    margin: 0;
    color: var(--muted);
    font-size: 0.81rem;
    line-height: 1.45;
  }

  @media (min-width: 640px) {
    .hero {
      padding-bottom: 2.25rem;
    }

    .hero-lede {
      font-size: 1.04rem;
    }
  }
</style>
