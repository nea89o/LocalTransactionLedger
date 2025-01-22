import { createAsync, useParams } from "@solidjs/router"
import { client, getAnalysisList, paths } from "./api.ts";
import { createSignal, For, onMount, Show, Suspense } from "solid-js";
import { SolidApexCharts } from "solid-apexcharts";

type AnalysisResult =
    { status: 'not requested' }
    | { status: 'loading' }
    | { status: 'loaded', result: paths['/analysis/execute']['get']['responses'][200]['content']['application/json'] }

export default function Analysis() {
    const pathParams = useParams();
    const analysisId = pathParams.id!;
    let analysis = createAsync(() => getAnalysisList());
    const analysisName = () => analysis()?.data?.find(it => it.id == analysisId)?.name
    const [startTimestamp, setStartTimestamp] = createSignal(new Date().getTime() - 1000 * 60 * 60 * 24 * 30);
    const [endTimestamp, setEndTimestamp] = createSignal(new Date().getTime());
    const [analysisResult, setAnalysisResult] = createSignal<AnalysisResult>({ status: 'not requested' });
    return <>
        <h1><Suspense fallback="Name not loaded...">{analysisName()}</Suspense></h1>
        <p>
            <label>
                Start:
                <input type="date" value={new Date(startTimestamp()).toISOString().substring(0, 10)} onInput={it => setStartTimestamp(it.target.valueAsNumber)}></input>
            </label>
            <label>
                End:
                <input type="date" value={new Date(endTimestamp()).toISOString().substring(0, 10)} onInput={it => setEndTimestamp(it.target.valueAsNumber)}></input>
            </label>
            <button disabled={analysisResult().status === 'loading'} onClick={() => {
                setAnalysisResult({ status: 'loading' });
                (async () => {
                    const result = await client.GET('/analysis/execute', {
                        params: {
                            query: {
                                analysis: analysisId,
                                tEnd: endTimestamp(),
                                tStart: startTimestamp()
                            }
                        }
                    });
                    setAnalysisResult({
                        status: "loaded",
                        result: result.data!
                    });
                })();
            }}>
                Refresh
            </button>

            <Show when={takeIf(analysisResult(), it => it.status == 'loaded')}>
                {element =>
                    <For each={element().result.visualizations}>
                        {item =>
                            <div>
                                <SolidApexCharts
                                    width={1200}
                                    type="bar"
                                    options={{
                                        xaxis: {
                                            type: 'numeric'
                                        }
                                    }}
                                    series={[
                                        {
                                            name: item.label,
                                            data: item.dataPoints.map(it => ([it.time, it.value]))
                                        }
                                    ]}
                                ></SolidApexCharts>
                            </div>
                        }
                    </For>}
            </Show>
        </p >
    </>
}

function takeIf<T extends P, P>(
    obj: P,
    condition: (arg: P) => arg is T,
): T | false {
    return condition(obj) ? obj : false;
}