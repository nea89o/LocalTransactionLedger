import createClient from "openapi-fetch";
import type { paths } from "./api-schema.js";

const apiRoot = import.meta.env.DEV ? "//localhost:8080/api" : "/api";

export const client = createClient<paths>({ baseUrl: apiRoot });
