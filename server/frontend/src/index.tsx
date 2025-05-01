/* @refresh reload */
import { render } from "solid-js/web";
import 'solid-devtools';

import "./index.css";
import type { RouteDefinition } from "@solidjs/router";
import { Router } from "@solidjs/router";
import { lazy, onMount } from "solid-js";

const root = document.getElementById("root");

if (!(root instanceof HTMLElement)) {
  throw new Error(
    "Root element not found. Did you forget to add it to your index.html? Or maybe the id attribute got misspelled?"
  );
}
const routes: Array<RouteDefinition> = [
  { path: "/", component: lazy(() => import("./App.tsx")) },
  { path: "/test/", component: lazy(() => import("./Test.tsx")) },
  { path: "/analysis/:id", component: lazy(() => import("./Analysis.tsx")) },
];

const Root = () => {

  return <div class="bg-gray-800 text-white min-h-[100vh]">
    <Router>{routes}</Router>
  </div>
}

render(() => <Root />, root!);
