import { For, Suspense, type Component } from "solid-js";
import { A, createAsync } from "@solidjs/router";
import { client, getAnalysisList } from "./api.ts";

const App: Component = () => {
  let analysis = createAsync(() => getAnalysisList());
  return (
    <>
      <Suspense fallback="Loading analysis...">
        <ul>
          <For each={analysis()?.data}>
            {item =>
              <li><A href={`/analysis/${item.id}`}>{item.name}</A></li>
            }
          </For>
        </ul>
      </Suspense>
    </>
  );
};

export default App;
