import { env } from '$env/dynamic/public';

const DEFAULT_API_URL = 'https://secret-santa-organizerv3.onrender.com';

/**
 * Base URL of the Spring Boot API.
 * Override it with the PUBLIC_API_URL environment variable.
 */
export const API_URL = (env.PUBLIC_API_URL || DEFAULT_API_URL).replace(/\/+$/, '');
