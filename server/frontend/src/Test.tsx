import {A, createAsync} from "@solidjs/router";
import {client} from "./api.js";
import {For, Suspense} from "solid-js";

export default function Test() {
    let items = createAsync(() => client.GET("/item", {
        params: {
            query: {
                itemId: ['HYPERION', 'BAT_WAND']
            }
        }
    }))
    return <>
        Test page <A href={"/"}>Back to main</A>
        <hr/>
        <Suspense fallback={"Loading items..."}>
            <p>Here are all Items:</p>
            <For each={Object.entries(items()?.data || {})}>
                { ([id, name]) => <li><code>{id}</code>: {name}</li>}
            </For>
        </Suspense>
    </>
}