import json, urllib.request, urllib.parse, sys
UA = "SwaraMusicProbe/1.0 (https://github.com/amitbarnwal125/Repository1)"
API = "https://commons.wikimedia.org/w/api.php"
def q(**p):
    p.setdefault("format", "json"); p.setdefault("formatversion", "2")
    url = API + "?" + urllib.parse.urlencode(p)
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    return json.load(urllib.request.urlopen(req, timeout=60))
def info(titles):
    out = []
    for i in range(0, len(titles), 40):
        r = q(action="query", titles="|".join(titles[i:i+40]), prop="imageinfo",
              iiprop="url|size|mime|mediatype|extmetadata", iiextmetadatafilter="LicenseShortName|Artist|ObjectName|DateTimeOriginal|ImageDescription")
        for pg in r["query"]["pages"]:
            ii = (pg.get("imageinfo") or [{}])[0]; md = ii.get("extmetadata", {})
            g = lambda k: (md.get(k, {}).get("value", "") or "")[:90].replace("\n", " ")
            out.append(f'{pg["title"]} | {ii.get("mime")} | {ii.get("size")} | dur={ii.get("duration")} | {g("LicenseShortName")} | {g("Artist")}')
    return out
print("=== SEARCHES ===")
for s in ["hindi song", "bollywood", "hindi film song", "raga", "sitar", "tabla", "bhajan", "ghazal", "qawwali", "shehnai", "bansuri", "santoor", "Indian music", "Hindustani", "Carnatic", "filmi", "Lata Mangeshkar", "K. L. Saigal", "harmonium", "dhol", "bhangra", "sarod", "veena", "indian folk song"]:
    try:
        r = q(action="query", list="search", srnamespace="6", srsearch="filetype:audio " + s, srlimit="25", srinfo="totalhits")
        hits = r["query"]["searchinfo"]["totalhits"]; titles = [x["title"] for x in r["query"]["search"]]
        print(f"\n--- '{s}' totalhits={hits}")
        for line in info(titles): print("  ", line)
    except Exception as e:
        print("ERR", s, e)
print("\n=== CATEGORY SEARCH ===")
for s in ["Audio files of Indian music", "Hindustani classical music audio", "Indian film songs", "Sitar audio", "Audio files Carnatic", "Music of India audio", "Bollywood music", "Indian classical music recordings", "Tabla audio"]:
    r = q(action="query", list="search", srnamespace="14", srsearch=s, srlimit="12")
    print(f"\n--- cat '{s}'")
    for x in r["query"]["search"]:
        t = x["title"]
        ci = q(action="query", titles=t, prop="categoryinfo")["query"]["pages"][0].get("categoryinfo", {})
        print("   ", t, ci)
