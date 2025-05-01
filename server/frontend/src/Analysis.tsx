import { createAsync, useParams } from "@solidjs/router"
import { client, getAnalysisList, paths } from "./api.ts";
import { createSignal, For, onMount, Show, Suspense } from "solid-js";
import { SolidApexCharts } from "solid-apexcharts";
import { BarChart, times } from "chartist";

type AnalysisResult =
    { status: 'not requested' }
    | { status: 'loading' }
    | { status: 'loaded', result: paths['/analysis/execute']['get']['responses'][200]['content']['application/json'] }

export default function Analysis() {
    const pathParams = useParams();
    const analysisId = pathParams.id!;
    let analysis = createAsync(() => getAnalysisList());
    const analysisName = () => analysis()?.data?.find(it => it.id == analysisId)?.name
    const [startTimestamp, setStartTimestamp] = createSignal(new Date().getTime() - 1000 * 60 * 60 * 24 * 356);
    const [endTimestamp, setEndTimestamp] = createSignal(new Date().getTime());
    const [analysisResult, setAnalysisResult] = createSignal<AnalysisResult>({ status: 'not requested' });
    return <>
        <h1 class="text-xl"><Suspense fallback="Name not loaded...">{analysisName()}</Suspense></h1>
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
                            <div class="h-300 max-h-[90vh]">
                                <SolidApexCharts
                                    type="bar"
                                    width={"100%"}
                                    height={"100%"}
                                    options={{
                                        colors: ['#b03060'],
                                        xaxis: {
                                            labels: {
                                                style: {
                                                    colors: '#A0A0A0',
                                                },
                                                formatter(value, timestamp, opts) {
                                                    return formatDate(timestamp!)
                                                },
                                            },
                                            type: 'numeric',
                                        },
                                        tooltip: {
                                            enabled: false
                                        },
                                        yaxis: {
                                            labels: {
                                                style: {
                                                    colors: '#A0A0A0'
                                                },
                                                formatter(val, opts) {
                                                    return formatMoney(val)
                                                }
                                            },
                                            decimalsInFloat: 3
                                        },
                                        dataLabels: {
                                            formatter(val, opts) {
                                                return formatMoney(val as number)
                                            },
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

const formatMoney = (money: number): string => {
    if (money < 0) return `-${formatMoney(money)}`
    const moneyNames = [
        [1_000_000_000, 'b'],
        [1_000_000, 'm'],
        [1_000, 'k'],
        [1, '']
    ] as const;

    for (const [factor, name] of moneyNames) {
        if (money >= factor) {
            const scaledValue = Math.round(money / factor * 10) / 10
            return `${scaledValue}${name}`
        }
    }
    return money.toString()
}

const formatDate = (date: number | Date) => {
    const _date = new Date(date);
    return `${_date.getDay()}.${_date.getMonth() + 1}.${_date.getFullYear()}`
}

function takeIf<T extends P, P>(
    obj: P,
    condition: (arg: P) => arg is T,
): T | false {
    return condition(obj) ? obj : false;
}