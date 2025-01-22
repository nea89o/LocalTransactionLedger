import createClient from "openapi-fetch";
import type { paths } from "./api-schema.js";
import { query } from "@solidjs/router";
export { type paths };

const apiRoot = import.meta.env.DEV ? "//localhost:8080/api" : "/api";

export const client = createClient<paths>({ baseUrl: apiRoot });

export const getAnalysisList = query(
    () => client.GET("/analysis/list"),
    "getAnalysisList"
)
