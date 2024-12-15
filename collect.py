import json
from pathlib import Path

repo = Path("/home/nea/src/NotEnoughUpdates-REPO")
l = {}
for f in (repo / "items").glob("*.json"):
    with  f.open('r') as fp:
        j = json.load(fp)
    l[j["internalname"]] = j["displayname"]
with Path("./compiled.json").open("w") as fp:
    json.dump(l, fp)
