import json, urllib.request, urllib.parse
UA = "SwaraMusicProbe/1.0 (https://github.com/amitbarnwal125/Repository1)"
API = "https://commons.wikimedia.org/w/api.php"
def q(**p):
    p.setdefault("format", "json"); p.setdefault("formatversion", "2")
    req = urllib.request.Request(API + "?" + urllib.parse.urlencode(p), headers={"User-Agent": UA})
    return json.load(urllib.request.urlopen(req, timeout=60))
cats = ["Audio files of Hindustani music","Audio files of Carnatic music","Audio files of Rabindra Sangeet","Qawwali","Bansuri","Ravi Shankar","Lavani","Girija Devi","Tabla","Mohan veena","Tambura","Audio files of music of India","Hindol Raga","Kaushiki Chakrabarty","Anjanibai Malpekar","Keshavrao Bhole","Sandip Ray","Dholak","Audio files of Indian film music","Indian film music","Film songs of India","Audio files of Bengali music","Songs in Hindi","Audio files in Hindi","Bhajans","Ghazals","Sitar music","Audio files of sitar","Shehnai","Audio files of folk music of India","Antti Luode","Kevin MacLeod"]
for c in cats:
    try:
        r = q(action="query", generator="categorymembers", gcmtitle="Category:"+c, gcmtype="file", gcmlimit="100", prop="imageinfo", iiprop="mediatype|mime|size")
        pages = r.get("query", {}).get("pages", [])
        aud = [p for p in pages if (p.get("imageinfo") or [{}])[0].get("mediatype") == "AUDIO"]
        sub = q(action="query", list="categorymembers", cmtitle="Category:"+c, cmtype="subcat", cmlimit="20").get("query",{}).get("categorymembers",[])
        print(f"CAT {c}: files={len(pages)} audio={len(aud)} subcats={[s['title'] for s in sub][:8]}")
        for p in aud[:6]: print("     ", p["title"], (p["imageinfo"][0]).get("mime"))
    except Exception as e: print("ERR", c, e)
