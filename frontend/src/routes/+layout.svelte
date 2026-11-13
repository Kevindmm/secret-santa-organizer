<script lang="ts">
	import '../app.css';
	import favicon from '$lib/assets/favicon.svg';
	import BackendStatus from '$lib/components/BackendStatus.svelte';
	import { API_URL } from '$lib/config';

	let { children } = $props();
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<!-- Open the connection to the backend before the health request, so the
	     free Render service wakes up as early as possible. -->
	<link rel="preconnect" href={API_URL} crossorigin="anonymous" />
	<meta
		name="description"
		content="Organiza tu amigo invisible en minutos: crea el intercambio, comparte el código y deja que el sorteo y los emails se encarguen del resto."
	/>
</svelte:head>

<!-- Mounted before the content so the health request starts on page load. -->
<BackendStatus />
{@render children()}
