import urllib.request
import re

req = urllib.request.Request(
    'https://filmrooz.top/',
    headers={'User-Agent': 'Mozilla/5.0'}
)
try:
    html = urllib.request.urlopen(req).read().decode('utf-8')
    print("Fetched index. Searching for X-Men...")
    links = re.findall(r'href="(https://filmrooz.top/series/[^"]+)"', html)
    print("Found series links:", list(set(links))[:5])
except Exception as e:
    print("Error:", e)
