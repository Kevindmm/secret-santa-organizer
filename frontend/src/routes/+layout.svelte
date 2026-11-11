<script lang="ts">
	import '../app.css';
	import favicon from '$lib/assets/favicon.svg';
	import BackendStatus from '$lib/components/BackendStatus.svelte';

	let { children } = $props();
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<meta
		name="description"
		content="Organiza tu amigo invisible en minutos: crea el intercambio, comparte el código y deja que el sorteo y los emails se encarguen del resto."
	/>
</svelte:head>

{@render children()}
<BackendStatus />
