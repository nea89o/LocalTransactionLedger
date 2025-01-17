import type {Component} from "solid-js";
import {A} from "@solidjs/router";

const App: Component = () => {
    return <>
        Hello World
        <A href="/test">Test Page</A>
    </>;
};

export default App;
