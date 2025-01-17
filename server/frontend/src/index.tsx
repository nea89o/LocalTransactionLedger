/* @refresh reload */
import { render } from "solid-js/web";

import "./index.css";
import type { RouteDefinition } from "@solidjs/router";
import { Router } from "@solidjs/router";
import { lazy } from "solid-js";

const root = document.getElementById("root");

if (!(root instanceof HTMLElement)) {
  throw new Error(
    "Root element not found. Did you forget to add it to your index.html? Or maybe the id attribute got misspelled?"
  );
}
const routes: Array<RouteDefinition> = [
  { path: "/", component: lazy(() => import("./App.tsx")) },
  { path: "/test/", component: lazy(() => import("./Test.tsx")) },
];

render(() => <Router>{routes}</Router>, root!);
